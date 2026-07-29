package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventBookmark;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventBookmarkServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long USER_ID = 100L;

    @Mock EventBookmarkRepository eventBookmarkRepository;
    @Mock EventRepository eventRepository;

    @InjectMocks EventBookmarkService service;

    @Test
    void toggleBookmark_북마크_없으면_생성() {
        when(eventBookmarkRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event()));

        boolean result = service.toggleBookmark(EVENT_ID, USER_ID);

        assertThat(result).isTrue();
        verify(eventBookmarkRepository).save(any(EventBookmark.class));
    }

    @Test
    void toggleBookmark_북마크가_있으면_삭제() {
        EventBookmark existing = new EventBookmark(event(), USER_ID);
        when(eventBookmarkRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.of(existing));

        boolean result = service.toggleBookmark(EVENT_ID, USER_ID);

        assertThat(result).isFalse();
        verify(eventBookmarkRepository).delete(existing);
        verify(eventRepository, never()).findNotDeletedById(anyLong());
    }

    @Test
    void toggleBookmark_행사가_없으면_예외() {
        when(eventBookmarkRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleBookmark(EVENT_ID, USER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
        verify(eventBookmarkRepository, never()).save(any());
    }

    @Test
    void removeBookmarksForEvent_행사의_북마크_삭제() {
        service.removeBookmarksForEvent(EVENT_ID);

        verify(eventBookmarkRepository).deleteAllByEventId(EVENT_ID);
    }

    private Event event() {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, 999L);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }
}