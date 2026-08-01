package com.coderhan.lastmission.reservation.application;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

        emitterManager.send(userId, "rank", waitingRoomQueue.getRank(userId, eventId));

        return emitter;
    }

    public void admitAll() {
        for (Long eventId : waitingRoomQueue.getActiveEventIds()) {
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
        if (emitterManager.isCurrent(userId, emitter)) {
            waitingRoomQueue.leave(userId, eventId);
        }
    }
}
