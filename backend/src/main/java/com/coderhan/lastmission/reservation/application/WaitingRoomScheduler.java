package com.coderhan.lastmission.reservation.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingRoomScheduler {

    private final WaitingRoomService waitingRoomService;

    @Scheduled(fixedRate = 1000)
    public void admit() {
        waitingRoomService.admitAll();
    }
}
