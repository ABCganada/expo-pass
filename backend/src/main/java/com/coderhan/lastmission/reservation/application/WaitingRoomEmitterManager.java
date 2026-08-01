package com.coderhan.lastmission.reservation.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WaitingRoomEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(long userId) {

        // 기존 연결이 있으면 종료 (이미 끊긴 연결이면 complete()가 IllegalStateException을 던질 수 있어 무시)
        SseEmitter oldEmitter = emitters.remove(userId);
        log.info("[waiting-room] connect userId={} 기존emitter있음={}", userId, oldEmitter != null);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 완료/끊긴 연결 — 새 연결로 교체하는 중이므로 무시
            }
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("[waiting-room] emitter onCompletion userId={}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.info("[waiting-room] emitter onTimeout userId={}", userId);
            emitters.remove(userId);
        });
        emitter.onError(e -> {
            log.info("[waiting-room] emitter onError userId={} error={}", userId, e.toString());
            emitters.remove(userId);
        });

        return emitter;
    }

    public void send(long userId, String eventName, Object data) {

        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.warn("[waiting-room] send 실패: 등록된 emitter 없음 userId={} event={}", userId, eventName);
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(data)
            );
        } catch (Exception e) {
            log.warn("[waiting-room] send 실패: userId={} event={} 연결 정리함", userId, eventName, e);
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
