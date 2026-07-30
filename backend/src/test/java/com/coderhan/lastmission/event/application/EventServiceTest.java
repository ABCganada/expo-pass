package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.coderhan.lastmission.event.ReservationQueryPort;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;

    @Mock EventRepository eventRepository;
    @Mock EventCategoryRepository eventCategoryRepository;
    @Mock EventBookmarkService eventBookmarkService;
    @Mock ReservationQueryPort reservationQueryPort;
    @Mock UserDirectory userDirectory;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks EventService service;

    @Test
    void deleteEvent_소프트삭제_후_북마크_삭제() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        service.deleteEvent(EVENT_ID);

        assertThat(event.isDeleted()).isTrue();
        verify(eventBookmarkService).removeBookmarksForEvent(EVENT_ID);
    }

    @Test
    void deleteEvent_예약_이력이_있으면_거부되고_삭제되지_않음() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(reservationQueryPort.hasActiveReservationsForEvent(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEvent(EVENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        assertThat(event.isDeleted()).isFalse();
        verify(eventBookmarkService, never()).removeBookmarksForEvent(anyLong());
    }

    @Test
    void deleteEvent_존재하지_않는_행사면_예외() {
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEvent(EVENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
        verify(eventBookmarkService, never()).removeBookmarksForEvent(anyLong());
    }

    private Event event(long managerId) {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, managerId);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }
}