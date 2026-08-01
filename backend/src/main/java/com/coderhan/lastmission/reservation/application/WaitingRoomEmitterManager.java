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

        // 기존 연결이 있으면 종료
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
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
            emitter.complete();
        }
    }

    public boolean isCurrent(long userId, SseEmitter emitter) {
        return emitters.get(userId) == emitter;
    }

    public Collection<SseEmitter> getAll() {
        return emitters.values();
    }
}
