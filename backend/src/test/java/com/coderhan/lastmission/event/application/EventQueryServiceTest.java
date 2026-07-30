package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.user.UserDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;
    private static final long OTHER_MANAGER_ID = 200L;

    @Mock EventRepository eventRepository;
    @Mock UserDirectory userDirectory;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks EventQueryService service;

    @Test
    void getAdminEvents_ADMIN은_전체_조회() {
        Event own = event(MANAGER_ID);
        Event other = event(OTHER_MANAGER_ID);
        when(eventRepository.findAllOrderByStartDateAsc()).thenReturn(List.of(own, other));

        List<EventQueryService.EventListItem> events = service.getAdminEvents(MANAGER_ID, true);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(own, other);
    }

    @Test
    void getAdminEvents_MANAGER는_본인_담당_행사만_조회() {
        Event own = event(MANAGER_ID);
        Event other = event(OTHER_MANAGER_ID);
        when(eventRepository.findAllOrderByStartDateAsc()).thenReturn(List.of(own, other));

        List<EventQueryService.EventListItem> events = service.getAdminEvents(MANAGER_ID, false);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(own);
    }

    private Event event(long managerId) {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, managerId);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        ReflectionTestUtils.setField(event, "startDate", LocalDate.parse("2026-08-01"));
        ReflectionTestUtils.setField(event, "endDate", LocalDate.parse("2026-08-31"));
        return event;
    }
}