package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 조회 전담 서비스 - 목록/상세 조회 (일반 사용자 + 관리자).
 */
@Service
@RequiredArgsConstructor
public class EventQueryService {
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketRepository ticketRepository;
    private final EventContentRepository eventContentRepository;
    private final EventImageRepository eventImageRepository;
    private final UserDirectory userDirectory;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EventListItem> getPublishedEvents(Long categoryId) {
        LocalDate today = LocalDate.now(clock);

        List<Event> events = categoryId == null
                ? eventRepository.findByStatusOrderByStartDateAsc(EventStatus.PUBLISHED)
                : eventRepository.findByStatusAndCategoryIdOrderByStartDateAsc(EventStatus.PUBLISHED, categoryId);
        Map<Long, String> thumbnailByEventId = thumbnailUrlsByEventIds(events);

        return events.stream()
                .map(event -> new EventListItem(event, event.phase(today), thumbnailByEventId.get(event.getId())))
                .toList();
    }

    /**
     * 관리자용 행사 목록 조회 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional(readOnly = true)
    public List<EventListItem> getAdminEvents(long callerUserId, boolean isAdmin, EventStatus status) {
        LocalDate today = LocalDate.now(clock);

        List<Event> events = isAdmin
                ? eventRepository.findAllOrderByStartDateAsc()
                : eventRepository.findAllByManagerIdOrderByStartDateAsc(callerUserId);
        events = events.stream()
                .filter(event -> status == null || event.getStatus() == status)
                .toList();
        Map<Long, String> thumbnailByEventId = thumbnailUrlsByEventIds(events);

        return events.stream()
                .map(event -> new EventListItem(event, event.phase(today), thumbnailByEventId.get(event.getId())))
                .toList();
    }

    /**
     * 행사 상세 조회.
     */
    @Transactional
    public EventDetail getEventDetail(long id, long viewerUserId) {
        LocalDate today = LocalDate.now(clock);

        Event event = eventRepository.findNotDeletedById(id)
                .filter(candidate -> candidate.getStatus() != EventStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        List<Ticket> tickets = ticketRepository.findAllNotDeletedByEventIdOrderByCreatedAtAsc(id);
        List<EventContent> contents = eventContentRepository.findAllByEventId(id);
        List<EventImage> images = eventImageRepository.findAllByEventIdOrderByDisplayOrderAsc(id);

        eventPublisher.publishEvent(new EventViewedEvent(id, viewerUserId));

        return new EventDetail(event, event.phase(today), tickets, contents, images);
    }

    /**
     * 관리자용 행사 상세 조회 - DRAFT도 조회 가능
     * ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional(readOnly = true)
    public AdminEventDetailResult getAdminEventDetail(long eventId, long callerUserId, boolean admin) {
        LocalDate today = LocalDate.now(clock);

        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, admin, "조회");

        UserRef manager = userDirectory.findActiveByIds(List.of(event.getManagerId()))
                .stream()
                .findFirst()
                .orElse(null);

        List<Ticket> tickets = ticketRepository.findAllByEventIdOrderByCreatedAtAsc(eventId);
        List<EventContent> contents = eventContentRepository.findAllByEventId(eventId);
        List<EventImage> images = eventImageRepository.findAllByEventIdOrderByDisplayOrderAsc(eventId);

        return new AdminEventDetailResult(event, event.phase(today), manager, tickets, contents, images);
    }

    /** 목록 조회용 썸네일을 이벤트당 1건씩 배치로 조회 (N+1 방지). */
    private Map<Long, String> thumbnailUrlsByEventIds(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events
                .stream()
                .map(Event::getId)
                .toList();
        return eventImageRepository.findAllByEventIdInAndImageType(eventIds, EventImageType.THUMBNAIL)
                .stream()
                .collect(Collectors.toMap(
                        image -> image.getEvent().getId(),
                        EventImage::getImageUrl,
                        (first, second) -> first)); // 중복 처리
    }

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    public record EventListItem(Event event, EventPhase phase, String thumbnailUrl) {}

    public record EventDetail(
            Event event, EventPhase phase, List<Ticket> tickets, List<EventContent> contents, List<EventImage> images) {}

    public record AdminEventDetailResult(
            Event event, EventPhase phase, UserRef manager,
            List<Ticket> tickets, List<EventContent> contents, List<EventImage> images) {}
}