package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.coderhan.lastmission.event.application.command.UpdateEventCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import com.coderhan.lastmission.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 목록 조회 - PUBLISHED만 조회
 */
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final UserDirectory userDirectory;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EventListItem> getPublishedEvents() {
        LocalDate today = LocalDate.now(clock);
        return eventRepository.findByStatusOrderByStartDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(event -> new EventListItem(event, event.phase(today)))
                .toList();
    }

    /**
     * 행사 상세 조회
     */
    @Transactional(readOnly = true)
    public EventDetail getEventDetail(long id) {
        Event event = eventRepository.findNotDeletedById(id)
                .filter(candidate -> candidate.getStatus() != EventStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        return new EventDetail(event, event.phase(LocalDate.now(clock)));
    }

    /**
     * 행사 생성 - SUPER_ADMIN 전용
     */
    @Transactional
    public Event createDraftEvent(String title, long categoryId, long managerId) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "제목은 비어 있을 수 없습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        userDirectory.findActiveAccessById(managerId)
                .filter(access -> access.roles().contains(UserRole.MANAGER))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EVENT_MANAGER_NOT_FOUND, "MANAGER 권한을 가진 담당자를 찾을 수 없습니다."));
        return eventRepository.save(new Event(title, category, managerId));
    }

    /**
     * 관리자용 행사 상세 조회 - DRAFT도 조회 가능
     * ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional(readOnly = true)
    public AdminEventDetailResult getAdminEventDetail(long eventId, long callerUserId, boolean admin) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, admin, "조회");
        UserRef manager = userDirectory.findActiveByIds(List.of(event.getManagerId()))
                .stream()
                .findFirst()
                .orElse(null);
        return new AdminEventDetailResult(event, manager);
    }

    /**
     * 행사 필드 수정 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public Event updateEvent(long eventId, long callerUserId, boolean admin, UpdateEventCommand command) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, admin, "수정");
        if (command.title() == null || command.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "제목은 비어 있을 수 없습니다.");
        }
        if (command.startDate() != null && command.endDate() != null
                && command.endDate().isBefore(command.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (command.legalDongCode() != null && command.legalDongCode().length() > 10) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "법정동코드는 10자를 초과할 수 없습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        event.updateDetails(command.title(), category, command.hostName(), command.venueName(),
                command.address(), command.detailAddress(), command.kakaoPlaceId(), command.legalDongCode(),
                command.latitude(), command.longitude(), command.startDate(), command.endDate());
        return event;
    }

    /** 
     * 행사 삭제 - SUPER_ADMIN 전용 
     */
    @Transactional
    public void deleteEvent(long eventId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        event.softDelete(Instant.now(clock));
    }

    /**
     * 행사 상태 변경 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public Event changeStatus(long eventId, long callerUserId, boolean isAdmin, EventStatus targetStatus) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, isAdmin, "상태를 변경");
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "취소된 행사는 상태를 변경할 수 없습니다.");
        }
        switch (targetStatus) {
            case PUBLISHED -> {
                validatePublishable(event);
                event.publish();
            }
            case CANCELLED -> event.cancel();
            case DRAFT -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "DRAFT로는 되돌릴 수 없습니다.");
        }
        return event;
    }

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    private void validatePublishable(Event event) {
        if (isBlank(event.getHostName()) || isBlank(event.getVenueName()) || isBlank(event.getAddress())
                || isBlank(event.getLegalDongCode()) || event.getLatitude() == null || event.getLongitude() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "게시하려면 주최/장소명/주소/법정동코드/좌표/시작일/종료일이 모두 입력되어야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record EventListItem(Event event, EventPhase phase) {}

    public record EventDetail(Event event, EventPhase phase) {}
    
    public record AdminEventDetailResult(Event event, UserRef manager) {}
}