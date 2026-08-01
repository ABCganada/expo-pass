package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long OTHER_EVENT_ID = 2L;
    private static final long MANAGER_ID = 100L;
    private static final long OTHER_MANAGER_ID = 200L;
    private static final long VIEWER_USER_ID = 300L;

    @Mock EventRepository eventRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TicketRepository ticketRepository;
    @Mock EventContentRepository eventContentRepository;
    @Mock EventImageRepository eventImageRepository;
    @Mock UserDirectory userDirectory;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks EventQueryService service;

    @Test
    void getAdminEvents_ADMIN은_전체_조회() {
        Event own = event(EVENT_ID, MANAGER_ID);
        Event other = event(OTHER_EVENT_ID, OTHER_MANAGER_ID);
        when(eventRepository.findAllOrderByStartDateAsc()).thenReturn(List.of(own, other));

        List<EventQueryService.EventListItem> events = service.getAdminEvents(MANAGER_ID, true, null);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(own, other);
    }

    @Test
    void getAdminEvents_MANAGER는_본인_담당_행사만_조회() {
        Event own = event(EVENT_ID, MANAGER_ID);
        when(eventRepository.findAllByManagerIdOrderByStartDateAsc(MANAGER_ID)).thenReturn(List.of(own));

        List<EventQueryService.EventListItem> events = service.getAdminEvents(MANAGER_ID, false, null);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(own);
    }

    @Test
    void getAdminEvents_status가_있으면_해당_상태만_조회() {
        Event draft = event(EVENT_ID, MANAGER_ID);
        Event published = event(OTHER_EVENT_ID, MANAGER_ID);
        ReflectionTestUtils.setField(published, "status", EventStatus.PUBLISHED);
        when(eventRepository.findAllOrderByStartDateAsc()).thenReturn(List.of(draft, published));

        List<EventQueryService.EventListItem> events = service.getAdminEvents(MANAGER_ID, true, EventStatus.PUBLISHED);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(published);
    }

    @Test
    void getPublishedEvents_행사별_썸네일을_배치로_채우기() {
        Event withThumb = event(EVENT_ID, MANAGER_ID);
        Event withoutThumb = event(OTHER_EVENT_ID, OTHER_MANAGER_ID);
        when(eventRepository.findByStatusOrderByStartDateAsc(EventStatus.PUBLISHED))
                .thenReturn(List.of(withThumb, withoutThumb));
        EventImage thumbnail = new EventImage(withThumb, "https://bucket/thumb.png", EventImageType.THUMBNAIL, 0);
        when(eventImageRepository.findAllByEventIdInAndImageType(
                List.of(EVENT_ID, OTHER_EVENT_ID), EventImageType.THUMBNAIL))
                .thenReturn(List.of(thumbnail));

        List<EventQueryService.EventListItem> events = service.getPublishedEvents(null);

        assertThat(events).extracting(EventQueryService.EventListItem::thumbnailUrl)
                .containsExactly("https://bucket/thumb.png", null);
    }

    @Test
    void getPublishedEvents_categoryId가_있으면_카테고리로_필터링() {
        long categoryId = 5L;
        Event matched = event(EVENT_ID, MANAGER_ID);
        when(eventRepository.findByStatusAndCategoryIdOrderByStartDateAsc(EventStatus.PUBLISHED, categoryId))
                .thenReturn(List.of(matched));

        List<EventQueryService.EventListItem> events = service.getPublishedEvents(categoryId);

        assertThat(events).extracting(EventQueryService.EventListItem::event).containsExactly(matched);
    }

    @Test
    void getEventDetail_상세정보와_phase_포함() {
        Event event = event(EVENT_ID, MANAGER_ID);
        ReflectionTestUtils.setField(event, "status", EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        Ticket ticket = new Ticket(event, "일반", 10000, 100, 5,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"));
        EventContent content = new EventContent(event, EventContentType.DESCRIPTION, "설명");
        EventImage image = new EventImage(event, "https://bucket/a.png", EventImageType.GENERAL, 0);

        when(ticketRepository.findAllNotDeletedByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of(ticket));
        when(eventContentRepository.findAllByEventId(EVENT_ID)).thenReturn(List.of(content));
        when(eventImageRepository.findAllByEventIdOrderByDisplayOrderAsc(EVENT_ID)).thenReturn(List.of(image));

        EventQueryService.EventDetail detail = service.getEventDetail(EVENT_ID, VIEWER_USER_ID);

        assertThat(detail.tickets()).containsExactly(ticket);
        assertThat(detail.contents()).containsExactly(content);
        assertThat(detail.images()).containsExactly(image);
    }

    @Test
    void getEventDetail_조회시_EventViewedEvent를_발행() {
        Event event = event(EVENT_ID, MANAGER_ID);
        ReflectionTestUtils.setField(event, "status", EventStatus.PUBLISHED);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketRepository.findAllNotDeletedByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of());
        when(eventContentRepository.findAllByEventId(EVENT_ID)).thenReturn(List.of());
        when(eventImageRepository.findAllByEventIdOrderByDisplayOrderAsc(EVENT_ID)).thenReturn(List.of());

        service.getEventDetail(EVENT_ID, VIEWER_USER_ID);

        verify(eventPublisher).publishEvent(new EventViewedEvent(EVENT_ID, VIEWER_USER_ID));
    }

    @Test
    void getAdminEventDetail_상세정보와_phase_포함() {
        Event event = event(EVENT_ID, MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));
        when(userDirectory.findActiveByIds(List.of(MANAGER_ID))).thenReturn(List.of());

        Ticket deletedTicket = new Ticket(event, "얼리버드", 8000, 50, 2,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-06-30T00:00:00Z"));
        EventContent content = new EventContent(event, EventContentType.NOTICE, "공지");
        EventImage image = new EventImage(event, "https://bucket/b.png", EventImageType.THUMBNAIL, 0);

        // 관리자용은 삭제된 티켓도 포함하는 findAllByEventIdOrderByCreatedAtAsc를 사용한다.
        when(ticketRepository.findAllByEventIdOrderByCreatedAtAsc(EVENT_ID)).thenReturn(List.of(deletedTicket));
        when(eventContentRepository.findAllByEventId(EVENT_ID)).thenReturn(List.of(content));
        when(eventImageRepository.findAllByEventIdOrderByDisplayOrderAsc(EVENT_ID)).thenReturn(List.of(image));

        EventQueryService.AdminEventDetailResult detail =
                service.getAdminEventDetail(EVENT_ID, MANAGER_ID, true);

        assertThat(detail.phase()).isEqualTo(EventPhase.UPCOMING);
        assertThat(detail.tickets()).containsExactly(deletedTicket);
        assertThat(detail.contents()).containsExactly(content);
        assertThat(detail.images()).containsExactly(image);
    }

    @Test
    void getAdminEventDetail_MANAGER가_담당하지_않는_행사면_거부() {
        Event event = event(EVENT_ID, OTHER_MANAGER_ID);
        when(eventRepository.findNotDeletedById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.getAdminEventDetail(EVENT_ID, MANAGER_ID, false))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_ACCESS_DENIED));
    }

    private Event event(long id, long managerId) {
        EventCategory category = new EventCategory("MUSIC", "음악", true);
        Event event = new Event("테스트 행사", category, managerId);
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "startDate", LocalDate.parse("2026-08-01"));
        ReflectionTestUtils.setField(event, "endDate", LocalDate.parse("2026-08-31"));
        return event;
    }
}