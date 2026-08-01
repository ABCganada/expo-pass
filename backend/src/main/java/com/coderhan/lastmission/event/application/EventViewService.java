package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 조회수 중복 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventViewService {
    private static final Duration VIEW_DEDUP_TTL = Duration.ofHours(24);
    private static final String VIEW_KEY_PREFIX = "event:view";

    private final EventService eventService;
    private final RedissonClient redissonClient;
    private final Clock clock;

    /**
     * 오늘 처음 보는 유저면 조회수 증가 - 조회 중복 기준 : 날짜, eventId, userId
     */
    public void increaseIfNew(long eventId, long userId) {
        try {
            RSet<Long> viewers = redissonClient.getSet(dedupKey(eventId));
            if (!viewers.add(userId)) {
                return;
            }
            viewers.expire(VIEW_DEDUP_TTL);
            try {
                eventService.increaseViewCount(eventId);
            } catch (RuntimeException e) {
                // DB 반영이 실패했으면 dedup 마킹도 되돌려서 다음 요청에서 다시 시도할 수 있게 함
                viewers.remove(userId);
                throw e;
            }
        } catch (RuntimeException e) {
            log.warn("조회수 증가 실패. eventId={}, userId={}", eventId, userId, e);
        }
    }

    private String dedupKey(long eventId) {
        return VIEW_KEY_PREFIX + ":%d:%s".formatted(eventId, LocalDate.now(clock));
    }
}