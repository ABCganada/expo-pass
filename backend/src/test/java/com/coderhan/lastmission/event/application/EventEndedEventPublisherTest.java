package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.coderhan.lastmission.event.EventEndedEvent;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventEndedEventPublisherTest {
    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;
    private static final LocalDate END_DATE = LocalDate.parse("2026-07-29");

    @Mock EventRepository eventRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-30T00:05:00Z"), ZoneOffset.UTC);

    @InjectMocks EventEndedEventPublisher publisher;

    @Test
    void publishEndedEvents_PUBLISHED_이고_미통지면_발행하고_통지시각을_기록한다() {
        Event event = event(EventStatus.PUBLISHED, END_DATE, null);
        when(eventRepository.findEndedEventsNotNotified(LocalDate.parse("2026-07-29"))).thenReturn(List.of(event));

        publisher.publishEndedEvents();

        ArgumentCaptor<EventEndedEvent> captor = ArgumentCaptor.forClass(EventEndedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new EventEndedEvent(EVENT_ID, END_DATE));
        assertThat(event.getEndedNotifiedAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void publishEndedEvents_어제_날짜를_기준으로_조회한다() {
        when(eventRepository.findEndedEventsNotNotified(any())).thenReturn(List.of());

        publisher.publishEndedEvents();

        verify(eventRepository).findEndedEventsNotNotified(LocalDate.parse("2026-07-29"));
    }

    private Event event(EventStatus status, LocalDate endDate, Instant endedNotifiedAt) {
        Event event = new Event("테스트 행사", category(), MANAGER_ID);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        if (status == EventStatus.PUBLISHED || status == EventStatus.CANCELLED) {
            event.publish();
        }
        if (status == EventStatus.CANCELLED) {
            event.cancel();
        }
        ReflectionTestUtils.setField(event, "endDate", endDate);
        ReflectionTestUtils.setField(event, "endedNotifiedAt", endedNotifiedAt);
        return event;
    }

    private EventCategory category() {
        return new EventCategory("MUSIC", "음악", true);
    }
}