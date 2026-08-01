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
        waitingRoomQueue.register(userId, eventId);
        SseEmitter emitter = emitterManager.connect(userId);

        emitter.onCompletion(() -> leaveIfCurrent(userId, eventId, emitter));
        emitter.onTimeout(() -> leaveIfCurrent(userId, eventId, emitter));
        emitter.onError(e -> leaveIfCurrent(userId, eventId, emitter));

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

    private void leaveIfCurrent(long userId, long eventId, SseEmitter emitter) {
        boolean current = emitterManager.isCurrent(userId, emitter);
        log.info("[waiting-room] leaveIfCurrent 콜백 발동 userId={} eventId={} isCurrent={}", userId, eventId, current);
        if (current) {
            waitingRoomQueue.leave(userId, eventId);
        }
    }
}
