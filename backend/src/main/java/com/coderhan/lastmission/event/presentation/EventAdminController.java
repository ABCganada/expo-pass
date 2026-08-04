package com.coderhan.lastmission.event.presentation;

import java.util.List;
import java.util.Locale;
import com.coderhan.lastmission.event.application.EventEndedEventPublisher;
import com.coderhan.lastmission.event.application.EventQueryService;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.presentation.response.EventManagementDetailResponse;
import com.coderhan.lastmission.event.presentation.response.EventManagementListResponse;
import com.coderhan.lastmission.event.presentation.response.EventSummaryResponse;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventAdminController {
    private final EventService eventService;
    private final EventQueryService eventQueryService;
    private final EventEndedEventPublisher eventEndedEventPublisher;

    @GetMapping("/api/v1/admin/events")
    ApiResponse<List<EventManagementListResponse>> getAdminEvents(@RequestParam(required = false) String status) {
        List<EventManagementListResponse> events = eventQueryService
                .getEventsForAdmin(parseStatus(status))
                .stream()
                .map(EventManagementListResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    @GetMapping("/api/v1/admin/events/{eventId}")
    ApiResponse<EventManagementDetailResponse> getEventDetail(@PathVariable long eventId) {
        EventQueryService.EventManagementDetail detail = eventQueryService.getEventDetailForAdmin(eventId);
        return ApiResponse.success(EventManagementDetailResponse.from(detail));
    }

    @DeleteMapping("/api/v1/admin/events/{eventId}")
    ApiResponse<Void> deleteEvent(@PathVariable long eventId) {
        eventService.deleteEventAsAdmin(eventId);
        return ApiResponse.success("행사를 삭제했습니다.", null);
    }

    @PatchMapping("/api/v1/admin/events/{eventId}/manager")
    ApiResponse<EventSummaryResponse> changeManager(@PathVariable long eventId, @RequestBody ChangeManagerRequest request) {
        Event event = eventService.changeManager(eventId, request.managerId());
        return ApiResponse.success("담당자를 변경했습니다.", EventSummaryResponse.from(event));
    }

    @PatchMapping("/api/v1/admin/events/{eventId}/publish")
    ApiResponse<EventSummaryResponse> publishEvent(@PathVariable long eventId) {
        Event event = eventService.publishEvent(eventId);
        return ApiResponse.success("행사를 게시했습니다.", EventSummaryResponse.from(event));
    }

    @PatchMapping("/api/v1/admin/events/{eventId}/cancel")
    ApiResponse<EventSummaryResponse> cancelEvent(@PathVariable long eventId) {
        Event event = eventService.cancelEventAsAdmin(eventId);
        return ApiResponse.success("행사를 취소했습니다.", EventSummaryResponse.from(event));
    }

    /**
     * [임시] 시연/데모용 - 종료된 행사 감지 및 EventEndedEvent 발행을 즉시 실행
     * TODO 시연 이후 제거 대상
     */
    @PostMapping("/api/v1/admin/events/end-check")
    ApiResponse<Void> triggerEndCheck() {
        eventEndedEventPublisher.publishEndedEvents();
        return ApiResponse.success("종료 이벤트 발행을 실행했습니다.", null);
    }

    private static EventStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EventStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST,
                    "status 값이 올바르지 않습니다. (PUBLISHED, DRAFT, CANCELLED 중 하나여야 합니다.)");
        }
    }

    record ChangeManagerRequest(Long managerId) {
        ChangeManagerRequest {
            if (managerId == null || managerId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "담당자 id가 올바르지 않습니다.");
            }
        }
    }

}