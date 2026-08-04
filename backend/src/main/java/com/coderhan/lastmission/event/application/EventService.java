package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.Instant;

import com.coderhan.lastmission.event.ReservationQueryPort;
import com.coderhan.lastmission.event.application.command.UpdateEventCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 생성/수정/삭제/상태 변경 전담 서비스
 */
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventBookmarkService eventBookmarkService;
    private final ReservationQueryPort reservationQueryPort;
    private final UserDirectory userDirectory;
    private final EventOwnershipValidator ownershipValidator;
    private final Clock clock;

    /**
     * 행사 생성 - MANAGER 전용. 본인을 담당자로 자동 배정
     */
    @Transactional
    public Event createDraftEvent(String title, long categoryId, long managerId) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "제목은 비어 있을 수 없습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        return eventRepository.save(new Event(title, category, managerId));
    }

    /**
     * 담당자 재배정
     */
    @Transactional
    public Event changeManager(long eventId, long newManagerId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateManager(newManagerId);
        event.changeManager(newManagerId);
        return event;
    }

    /** 새 담당자가 활성 상태의 MANAGER 권한 보유 계정인지 확인 */
    private void validateManager(long managerId) {
        userDirectory.findActiveAccessById(managerId)
                .filter(access -> access.roles().contains(UserRole.MANAGER))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EVENT_MANAGER_NOT_FOUND, "MANAGER 권한을 가진 담당자를 찾을 수 없습니다."));
    }

    /**
     * 행사 필드 수정 - MANAGER 전용, 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public Event updateEventAsManager(long eventId, long callerUserId, UpdateEventCommand command) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        ownershipValidator.requireOwner(event, callerUserId, "수정");
        if (command.title() == null || command.title().isBlank()) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "제목은 비어 있을 수 없습니다.");
        }
        if (command.startDate() != null && command.endDate() != null
                && command.endDate().isBefore(command.startDate())) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (command.legalDongCode() != null && command.legalDongCode().length() > 10) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "법정동코드는 10자를 초과할 수 없습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        event.updateDetails(command.title(), category, command.hostName(), command.venueName(),
                command.address(), command.detailAddress(), command.kakaoPlaceId(), command.legalDongCode(),
                command.latitude(), command.longitude(), command.startDate(), command.endDate());
        return event;
    }

    /**
     * 행사 삭제 - MANAGER 전용, 본인이 담당하는 DRAFT 상태 행사만.
     */
    @Transactional
    public void deleteEventAsManager(long eventId, long callerUserId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        ownershipValidator.requireOwner(event, callerUserId, "삭제");
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException(ErrorCode.EVENT_DELETE_NOT_ALLOWED, "초안 상태의 행사만 삭제할 수 있습니다.");
        }
        deleteEvent(event);
    }

    /**
     * 행사 삭제 - ADMIN 전용, 전체 상태(활성 예약이 없는 경우에 한해).
     */
    @Transactional
    public void deleteEventAsAdmin(long eventId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        deleteEvent(event);
    }

    private void deleteEvent(Event event) {
        if (reservationQueryPort.hasActiveReservationsForEvent(event.getId())) {
            throw new BusinessException(ErrorCode.EVENT_DELETE_NOT_ALLOWED, "활성 예약이 있는 행사는 삭제할 수 없습니다.");
        }
        eventBookmarkService.removeBookmarksForEvent(event.getId());
        event.softDelete(Instant.now(clock));
    }

    /**
     * 게시 승인 - ADMIN 전용. DRAFT → PUBLISHED.
     */
    @Transactional
    public Event publishEvent(long eventId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "초안 상태의 행사만 게시할 수 있습니다.");
        }
        validatePublishable(event);
        event.publish();
        return event;
    }

    /**
     * 취소 - MANAGER 전용, 본인이 담당(manager_id)하는 행사만. PUBLISHED → CANCELLED.
     */
    @Transactional
    public Event cancelEventAsManager(long eventId, long callerUserId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        ownershipValidator.requireOwner(event, callerUserId, "취소");
        return cancelEvent(event);
    }

    /**
     * 취소 - ADMIN 전용, 전체. PUBLISHED → CANCELLED.
     */
    @Transactional
    public Event cancelEventAsAdmin(long eventId) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        return cancelEvent(event);
    }

    private Event cancelEvent(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "게시된 행사만 취소할 수 있습니다.");
        }
        if (event.isEnded(clock)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "이미 종료된 행사는 취소할 수 없습니다.");
        }
        event.cancel();
        return event;
    }

    /**
     * 조회수 증가.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseViewCount(long eventId) {
        int updated = eventRepository.increaseViewCount(eventId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다.");
        }
    }

    private void validatePublishable(Event event) {
        if (isBlank(event.getHostName()) || isBlank(event.getVenueName()) || isBlank(event.getAddress())
                || isBlank(event.getLegalDongCode()) || event.getLatitude() == null || event.getLongitude() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST,
                    "게시하려면 주최/장소명/주소/법정동코드/좌표/시작일/종료일이 모두 입력되어야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}