package com.coderhan.lastmission.reservation.infrastructure.redis;

import com.coderhan.lastmission.reservation.application.WaitingRoomQueue;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWaitingRoomQueue implements WaitingRoomQueue {
    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "waiting_queue";
    private static final String SEQ_KEY = "queue_sequence";
    private static final String ADMITTED_KEY = "admitted";
    private static final String ACTIVE_EVENTS_KEY = "waiting_active_events";

    @Override
    public void register(long userId, long eventId) {
        // register~getRank(sendInitialRank) 사이의 창이 좁을수록 "등록 직후 스케줄러가
        // 먼저 허가해버리는" 레이스가 줄어든다. 여기서 순번을 다시 조회하거나 진단용으로
        // 전체 활성 이벤트 목록을 또 읽는 건 왕복만 늘릴 뿐 호출부(enter())가 반환값을
        // 쓰지도 않으므로 제거한다 — 순번 조회는 오직 sendInitialRank()에서만 한다.
        long sequence = redisTemplate.opsForValue().increment(SEQ_KEY + eventId);
        redisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY + eventId, String.valueOf(userId), sequence);
        redisTemplate.opsForSet().add(ACTIVE_EVENTS_KEY, String.valueOf(eventId));
        Long ttl = redisTemplate.getExpire(ACTIVE_EVENTS_KEY);
        if (ttl != null && ttl >= 0) {
            log.warn("[waiting-room] {} 키에 예상치 못한 TTL={}s 발견, PERSIST로 해제", ACTIVE_EVENTS_KEY, ttl);
            redisTemplate.persist(ACTIVE_EVENTS_KEY);
        }
    }

    @Override
    public long getRank(long userId, long eventId) {
        Long rank = redisTemplate.opsForZSet()
                .rank(QUEUE_KEY + eventId, String.valueOf(userId));
        if (rank == null) {
            throw new BusinessException(ErrorCode.WAITING_NOT_FOUND, "대기열에 존재하지 않는 사용자입니다.");
        }
        return rank + 1;
    }
    @Override
    public Map<Long, Long> getAllRanks(long eventId) {
        Set<ZSetOperations.TypedTuple<String>> all =
                redisTemplate.opsForZSet().rangeWithScores(QUEUE_KEY + eventId, 0, -1);
        if (all == null) return Map.of();

        Map<Long, Long> ranks = new LinkedHashMap<>();
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : all) {
            ranks.put(Long.parseLong(tuple.getValue()), rank++);
        }
        return ranks;
    }

    // 일단 단일 서버 가정하고 synchronized 적용, 추후 다중 서버일 때는 분산락이 Lua Script 적용 고려
    @Override
    public synchronized List<Long> allowEntry(long eventId, int count) {
        Set<String> users = redisTemplate.opsForZSet()
                .range(QUEUE_KEY + eventId, 0, count - 1);
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<Long> admittedUserIds = new ArrayList<>();
        for (String userId : users) {
            // 대기열 제거
            redisTemplate.opsForZSet().remove(QUEUE_KEY + eventId, userId);
            // 입장 티켓 발급 (5분 유효)
            redisTemplate.opsForValue().set(
                    admittedKey(eventId, userId),
                    "true",
                    Duration.ofMinutes(5)
            );
            admittedUserIds.add(Long.parseLong(userId));
        }
        removeFromActiveEventsIfEmpty(eventId);
        log.info("[waiting-room] allowEntry eventId={} admitted={}", eventId, admittedUserIds);
        return admittedUserIds;
    }

    @Override
    public boolean hasTicket(long userId, long eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(admittedKey(eventId, String.valueOf(userId))));
    }

    @Override
    public void consumeTicket(long userId, long eventId) {
        String key = admittedKey(eventId, String.valueOf(userId));

        String ticket = redisTemplate.opsForValue().getAndDelete(key);

        if (ticket == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ADMISSION_TICKET,
                    "입장 권한이 존재하지 않습니다."
            );
        }
    }

    @Override
    public void leave(long userId, long eventId) {
        redisTemplate.opsForZSet().remove(
                QUEUE_KEY + eventId,
                String.valueOf(userId)
        );
        removeFromActiveEventsIfEmpty(eventId);
    }

    @Override
    public Set<Long> getActiveEventIds() {
        Set<String> raw = redisTemplate.opsForSet().members(ACTIVE_EVENTS_KEY);
        if (raw == null) return Set.of();
        return raw.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    private void removeFromActiveEventsIfEmpty(long eventId) {
        Long remaining = redisTemplate.opsForZSet().zCard(QUEUE_KEY + eventId);
        if (remaining != null && remaining == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_EVENTS_KEY, String.valueOf(eventId));
        }
    }

    private String admittedKey(long eventId, String userId) {
        return ADMITTED_KEY + ":" + eventId + ":" + userId;
    }
}
