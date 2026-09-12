package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.coderhan.lastmission.event.EventEndedEvent;
import com.coderhan.lastmission.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventEndedEventPublisher {
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 종료됐지만 아직 통지하지 않은 PUBLISHED 행사를 찾아 {@link EventEndedEvent}를 발행
     */
    @Transactional
    public void publishEndedEvents() {
        LocalDate today = LocalDate.now(clock);
        eventRepository.findEndedEventsNotNotified(today)
                .forEach(this::notifyEnded);
    }

    private void notifyEnded(Event event) {
        event.markEndedNotified(Instant.now(clock));
        eventPublisher.publishEvent(
                new EventEndedEvent(event.getId(), event.getEndDate())
        );
    }
}