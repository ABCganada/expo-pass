package com.coderhan.lastmission.reservation.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class WaitingRoomPingScheduler {

    private final WaitingRoomEmitterManager emitterManager;

    @Scheduled(fixedRate = 20000)
    public void ping() {

        for (SseEmitter emitter : emitterManager.getAll()) {

            try {
                emitter.send(
                        SseEmitter.event()
                                .name("ping")
                                .data("")
                );
            } catch (IOException e) {
                emitter.complete();
            }
        }
    }
}
