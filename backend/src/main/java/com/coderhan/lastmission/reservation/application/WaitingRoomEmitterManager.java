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
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 완료/끊긴 연결 — 새 연결로 교체하는 중이므로 무시
            }
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);
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
            evict(userId, emitter);
        }
    }

    /**
     * 이 emitter가 지금도 이 유저의 "현재" 연결일 때만 원자적으로 제거한다.
     * 재연결 등으로 이미 다른 emitter로 교체된 뒤라면(맵의 값과 불일치) 아무 것도 하지 않고
     * false를 반환한다 — 뒤늦게 도착한 옛날 연결의 콜백이 방금 들어온 새 연결을 잘못
     * 쫓아내는 걸 막기 위한 원자적 compare-and-remove.
     */
    public boolean evict(long userId, SseEmitter emitter) {
        boolean removed = emitters.remove(userId, emitter);
        if (removed) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 이미 완료/끊긴 연결
            }
        }
        return removed;
    }

    public Collection<SseEmitter> getAll() {
        return emitters.values();
    }
}
