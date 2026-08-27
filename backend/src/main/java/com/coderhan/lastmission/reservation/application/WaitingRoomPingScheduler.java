package com.coderhan.lastmission.reservation.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
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
                completeQuietly(emitter);
            } catch (IllegalStateException e) {
                // send()도 이미 완료/에러 처리된 emitter에는 IllegalStateException을 던질 수 있다.
                completeQuietly(emitter);
            }
        }
    }

    /**
     * 클라이언트가 끊긴 시점에 톰캣이 우리보다 먼저 비동기 컨텍스트를 에러 처리해버리면,
     * complete() 호출 자체가 IllegalStateException으로 거부된다("non-container thread
     * attempted to use the AsyncContext after ... AsyncListener.onError() had returned").
     * 그 시점엔 이미 정리된 연결이라 무시해도 안전하지만, 여기서 던지게 두면 for 루프 자체가
     * 중단돼 같은 틱에 남아있던 다른 사용자 전원이 ping을 못 받으므로 반드시 개별 흡수한다.
     */
    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("[waiting-room] ping: 이미 정리된 연결의 complete() 호출 무시", e);
        }
    }
}
