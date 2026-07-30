package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 조회 전담 서비스 - 목록/상세 조회 (일반 사용자 + 관리자).
 */
@Service
@RequiredArgsConstructor
public class EventQueryService {
    private final EventRepository eventRepository;
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
     * 관리자용 행사 목록 조회 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional(readOnly = true)
    public List<EventListItem> getAdminEvents(long callerUserId, boolean isAdmin) {
        LocalDate today = LocalDate.now(clock);
        return eventRepository.findAllOrderByStartDateAsc()
                .stream()
                .filter(event -> isAdmin || Objects.equals(event.getManagerId(), callerUserId))
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

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    public record EventListItem(Event event, EventPhase phase) {}

    public record EventDetail(Event event, EventPhase phase) {}

    public record AdminEventDetailResult(Event event, UserRef manager) {}
}