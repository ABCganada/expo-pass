package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventContentServiceTest {

    private static final long EVENT_ID = 1L;
    private static final long MANAGER_ID = 100L;
    private static final long OTHER_MANAGER_ID = 200L;

    @Mock EventRepository eventRepository;
    @Mock EventContentRepository eventContentRepository;
    @Spy EventOwnershipValidator ownershipValidator = new EventOwnershipValidator();

    @InjectMocks EventContentService service;

    @Test
    void upsertContentsAsManager_기존_콘텐츠가_없으면_새로_생성() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventContentRepository.findByEventIdAndContentType(EVENT_ID, EventContentType.DESCRIPTION))
                .thenReturn(Optional.empty());
        when(eventContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EventContent> result = service.upsertContentsAsManager(
                EVENT_ID, MANAGER_ID, Map.of(EventContentType.DESCRIPTION, "새 설명"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getContent()).isEqualTo("새 설명");
        assertThat(result.getFirst().getContentType()).isEqualTo(EventContentType.DESCRIPTION);
    }

    @Test
    void upsertContentsAsManager_기존_콘텐츠가_있으면_업데이트() {
        Event event = event(MANAGER_ID);
        EventContent existing = new EventContent(event, EventContentType.NOTICE, "예전 공지");
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventContentRepository.findByEventIdAndContentType(EVENT_ID, EventContentType.NOTICE))
                .thenReturn(Optional.of(existing));

        List<EventContent> result = service.upsertContentsAsManager(
                EVENT_ID, MANAGER_ID, Map.of(EventContentType.NOTICE, "새 공지"));

        assertThat(result).containsExactly(existing);
        assertThat(existing.getContent()).isEqualTo("새 공지");
        // 관리 중인 엔티티 수정이므로 dirty checking에 맡기고 save()는 호출하지 않는다.
        verify(eventContentRepository, never()).save(any());
    }

    @Test
    void upsertContentsAsManager_여러_contentType을_한번에_처리() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventContentRepository.findByEventIdAndContentType(eq(EVENT_ID), any())).thenReturn(Optional.empty());
        when(eventContentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<EventContentType, String> contents = Map.of(
                EventContentType.DESCRIPTION, "설명",
                EventContentType.NOTICE, "공지",
                EventContentType.LOCATION_GUIDE, "오시는 길");

        List<EventContent> result = service.upsertContentsAsManager(EVENT_ID, MANAGER_ID, contents);

        assertThat(result).extracting(EventContent::getContentType)
                .containsExactlyInAnyOrder(EventContentType.DESCRIPTION, EventContentType.NOTICE, EventContentType.LOCATION_GUIDE);
    }

    @Test
    void upsertContentsAsManager_행사가_없으면_예외() {
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertContentsAsManager(
                EVENT_ID, MANAGER_ID, Map.of(EventContentType.DESCRIPTION, "설명")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    void upsertContentsAsManager_담당하지_않는_행사면_거부() {
        Event event = event(OTHER_MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.upsertContentsAsManager(
                EVENT_ID, MANAGER_ID, Map.of(EventContentType.DESCRIPTION, "설명")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_ACCESS_DENIED));
        verify(eventContentRepository, never()).save(any());
    }

    @Test
    void upsertContentsAsManager_콘텐츠_목록이_비어있으면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.upsertContentsAsManager(EVENT_ID, MANAGER_ID, Map.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
    }

    @Test
    void upsertContentsAsManager_content가_공백이면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.upsertContentsAsManager(
                EVENT_ID, MANAGER_ID, Map.of(EventContentType.DESCRIPTION, "  ")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        verify(eventContentRepository, never()).save(any());
    }

    @Test
    void upsertContentsAsManager_contents가_null이면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.upsertContentsAsManager(EVENT_ID, MANAGER_ID, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        verify(eventContentRepository, never()).save(any());
    }

    @Test
    void upsertContentsAsManager_contentType이_null이면_거부() {
        Event event = event(MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        Map<EventContentType, String> contents = new HashMap<>();
        contents.put(null, "설명");

        assertThatThrownBy(() -> service.upsertContentsAsManager(EVENT_ID, MANAGER_ID, contents))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_INVALID_REQUEST));
        verify(eventContentRepository, never()).save(any());
    }

    private Event event(long managerId) {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, managerId);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }
}