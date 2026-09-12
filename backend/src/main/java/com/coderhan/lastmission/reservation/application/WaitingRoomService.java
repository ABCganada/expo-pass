package com.coderhan.lastmission.reservation.application;

import java.util.List;
import java.util.Map;

import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomService {
    private static final int ADMIT_COUNT_PER_TICK = 10;

    private final WaitingRoomQueue waitingRoomQueue;
    private final WaitingRoomEmitterManager emitterManager;

    public SseEmitter enter(long userId, long eventId) {
        // emitter를 먼저 등록해야 "대기열엔 있는데 emitter는 아직 없는" 구간이 사라진다.
        // 이 순서가 뒤집혀 있으면, register() 직후 스케줄러가 바로 admitted를 보내려 할 때
        // emitterManager에 아직 emitter가 없어서 그 알림이 조용히 유실될 수 있었다.
        SseEmitter emitter = emitterManager.connect(userId);

        emitter.onCompletion(() -> leaveIfEvicted(userId, eventId, emitter));
        emitter.onTimeout(() -> leaveIfEvicted(userId, eventId, emitter));
        emitter.onError(e -> leaveIfEvicted(userId, eventId, emitter));

        // 이미 입장 허가를 받은 유저는 다시 줄 세우지 않는다.
        // admitted 알림은 그 시점에 연결을 쥐고 있던 프로세스의 메모리를 거쳐 나가므로,
        // 알림 순간 연결이 끊겨 있었거나 다른 인스턴스가 허가했다면 그대로 유실된다.
        // 그때 대기열엔 이미 없으니 아무도 다시 부르지 않아 영영 멈춘다 — 티켓(5분 유효)이
        // Redis에 남아 있으므로, 브라우저가 자동 재연결할 때 여기서 스스로 복구한다.
        if (waitingRoomQueue.hasTicket(userId, eventId)) {
            emitterManager.send(userId, "admitted", true);
            return emitter;
        }

        waitingRoomQueue.register(userId, eventId);

        sendInitialRank(userId, eventId);

        return emitter;
    }

    /**
     * register() 직후 대기자가 거의 없는 상태(로컬 테스트 등)에서는, getRank() 를 부르기
     * 전에 스케줄러(admitAll)가 그 사이 이 유저를 이미 허가해버리는 레이스가 발생할 수 있다.
     * 이 경우 대기열엔 이미 없으므로 WAITING_NOT_FOUND 가 나는데, 이건 실패가 아니라
     * "등록하자마자 바로 허가된 것"이므로 admitted 로 처리한다.
     */
    private void sendInitialRank(long userId, long eventId) {
        try {
            emitterManager.send(userId, "rank", waitingRoomQueue.getRank(userId, eventId));
        } catch (BusinessException e) {
            if (e.errorCode() != ErrorCode.WAITING_NOT_FOUND) {
                throw e;
            }
            emitterManager.send(userId, "admitted", true);
        }
    }

    public void admitAll() {
        var activeEventIds = waitingRoomQueue.getActiveEventIds();
        log.info("[waiting-room] admitAll tick activeEventIds={}", activeEventIds);
        for (Long eventId : activeEventIds) {
            List<Long> admittedUserIds = waitingRoomQueue.allowEntry(eventId, ADMIT_COUNT_PER_TICK);
            for (Long userId : admittedUserIds) {
                emitterManager.send(userId, "admitted", true);
            }
        }
    }

    public void consumeTicket(long userId, long eventId) {
        waitingRoomQueue.consumeTicket(userId, eventId);
    }

    public void broadcastRanks() {
        for (Long eventId : waitingRoomQueue.getActiveEventIds()) {
            Map<Long, Long> ranks = waitingRoomQueue.getAllRanks(eventId);
            ranks.forEach((userId, rank) -> emitterManager.send(userId, "rank", rank));
        }
    }

    private void leaveIfEvicted(long userId, long eventId, SseEmitter emitter) {
        // evict()가 원자적으로 "이 emitter가 여전히 현재 연결인지 확인 + 제거"를 한 번에 하므로,
        // 그 결과(true)만 있을 때 Redis 대기열도 같이 정리한다. 이미 새 연결로 교체된 뒤
        // 뒤늦게 도착한 콜백이면 evict()가 false를 반환하고 아무 것도 건드리지 않는다.
        if (emitterManager.evict(userId, emitter)) {
            waitingRoomQueue.leave(userId, eventId);
        }
    }
}
