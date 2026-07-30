package com.coderhan.lastmission.event.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEndScheduler {
    private final EventEndedEventPublisher eventEndedEventPublisher;

    /**
     * 매일 03:05에 종료된 행사를 감지해 EventEndedEvent를 발행
     */
    @Scheduled(cron = "0 5 3 * * *")
    public void detectEndedEvents() {
        eventEndedEventPublisher.publishEndedEvents();
    }
}