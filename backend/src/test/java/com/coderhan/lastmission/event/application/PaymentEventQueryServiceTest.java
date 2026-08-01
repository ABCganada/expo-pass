package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentEventQueryServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;

    @Mock EventRepository eventRepository;

    @InjectMocks PaymentEventQueryService service;

    @Test
    void findEventManagerId_존재하면_매니저id_반환() {
        Event event = new Event("테스트 행사", new EventCategory("MUSIC", "음악", true), MANAGER_ID);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThat(service.findEventManagerId(EVENT_ID)).contains(MANAGER_ID);
    }

    @Test
    void findEventManagerId_없거나_삭제됐으면_빈값() {
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.empty());

        assertThat(service.findEventManagerId(EVENT_ID)).isEmpty();
    }

    @Test
    void findEventIdsManagedBy_리포지토리_결과를_그대로_반환() {
        when(eventRepository.findIdsByManagerId(MANAGER_ID)).thenReturn(List.of(1L, 2L, 3L));

        assertThat(service.findEventIdsManagedBy(MANAGER_ID)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void findEventStartDate_존재하면_시작일_반환() {
        when(eventRepository.findStartDateByEventId(EVENT_ID)).thenReturn(Optional.of(LocalDate.of(2026, 8, 15)));

        assertThat(service.findEventStartDate(EVENT_ID)).contains(LocalDate.of(2026, 8, 15));
    }

    @Test
    void findEventStartDate_없거나_삭제됐으면_빈값() {
        when(eventRepository.findStartDateByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertThat(service.findEventStartDate(EVENT_ID)).isEmpty();
    }
}