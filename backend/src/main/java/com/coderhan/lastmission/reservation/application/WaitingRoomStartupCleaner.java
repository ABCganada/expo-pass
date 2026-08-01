package com.coderhan.lastmission.reservation.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버가 재시작되면 그 순간 대기 중이던 유저들의 SSE 연결이 전부 끊기고
 * (onCompletion/onTimeout/onError 콜백은 죽은 프로세스 안의 코드라 실행되지 않음),
 * 새로 뜬 프로세스는 그 유저들을 전혀 모른다. 그대로 두면 Redis 대기열에
 * 아무도 정리해줄 수 없는 유령 항목으로 영원히 남는다.
 *
 * <p>재시작 시 어차피 모든 대기자는 연결이 끊겨 재접속해야 하므로,
 * 시작 시점에 대기열 상태를 통째로 초기화한다.</p>
 */
@Component
@RequiredArgsConstructor
class WaitingRoomStartupCleaner {
    private final WaitingRoomQueue waitingRoomQueue;

    @EventListener(ApplicationReadyEvent.class)
    void clearStaleQueuesOnStartup() {
        waitingRoomQueue.clearAll();
    }
}
