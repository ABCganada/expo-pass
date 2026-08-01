package com.coderhan.lastmission.reservation.application;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WaitingRoomEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(long userId) {

        // 기존 연결이 있으면 종료 (이미 끊긴 연결이면 complete()가 IllegalStateException을 던질 수 있어 무시)
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 완료/끊긴 연결 — 새 연결로 교체하는 중이므로 무시
            }
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    public void send(long userId, String eventName, Object data) {

        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(data)
            );
        } catch (IOException e) {
            remove(userId);
        }
    }

    public void remove(long userId) {
        SseEmitter emitter = emitters.remove(userId);

        if (emitter != null) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 완료/끊긴 연결
            }
        }
    }

    public boolean isCurrent(long userId, SseEmitter emitter) {
        return emitters.get(userId) == emitter;
    }

    public Collection<SseEmitter> getAll() {
        return emitters.values();
    }
}
