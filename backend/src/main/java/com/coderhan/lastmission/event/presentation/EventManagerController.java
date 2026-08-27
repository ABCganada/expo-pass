package com.coderhan.lastmission.event.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import com.coderhan.lastmission.event.application.EventQueryService;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.application.command.UpdateEventCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.presentation.response.EventManagementDetailResponse;
import com.coderhan.lastmission.event.presentation.response.EventManagementListResponse;
import com.coderhan.lastmission.event.presentation.response.EventSummaryResponse;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
class EventManagerController {
    private final EventQueryService eventQueryService;
    private final EventService eventService;

    @PostMapping("/api/v1/manager/events")
    ResponseEntity<ApiResponse<EventSummaryResponse>> createDraftEvent(
            @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        Event event = eventService.createDraftEvent(request.title(), request.validateCategoryId(), principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(EventSummaryResponse.from(event)));
    }

    @GetMapping("/api/v1/manager/events")
    ApiResponse<List<EventManagementListResponse>> getManagerEvents(
            @AuthenticationPrincipal LastMissionPrincipal principal,
            @RequestParam(required = false) String status) {
        List<EventManagementListResponse> events = eventQueryService
                .getEventsForManager(principal.userId(), parseStatus(status))
                .stream()
                .map(EventManagementListResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    @GetMapping("/api/v1/manager/events/{eventId}")
    ApiResponse<EventManagementDetailResponse> getEventDetail(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        EventQueryService.EventManagementDetail detail =
                eventQueryService.getEventDetailForManager(eventId, principal.userId());
        return ApiResponse.success(EventManagementDetailResponse.from(detail));
    }

    @PatchMapping("/api/v1/manager/events/{eventId}")
    ApiResponse<EventSummaryResponse> updateEvent(@PathVariable long eventId,
            @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        Event event = eventService.updateEventAsManager(eventId, principal.userId(), request.toCommand());
        return ApiResponse.success(EventSummaryResponse.from(event));
    }

    @PatchMapping("/api/v1/manager/events/{eventId}/cancel")
    ApiResponse<EventSummaryResponse> cancelEvent(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        Event event = eventService.cancelEventAsManager(eventId, principal.userId());
        return ApiResponse.success(EventSummaryResponse.from(event));
    }

    @DeleteMapping("/api/v1/manager/events/{eventId}")
    ApiResponse<Void> deleteEvent(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        eventService.deleteEventAsManager(eventId, principal.userId());
        return ApiResponse.success("행사를 삭제했습니다.", null);
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

    record CreateEventRequest(String title, Long categoryId) {
        long validateCategoryId() {
            if (categoryId == null || categoryId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "카테고리 id가 올바르지 않습니다.");
            }
            return categoryId;
        }
    }

    record UpdateEventRequest(
            String title, Long categoryId, String hostName, String venueName,
            String address, String detailAddress, String kakaoPlaceId, String legalDongCode,
            BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate
    ) {
        UpdateEventCommand toCommand() {
            if (categoryId == null || categoryId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "카테고리 id가 올바르지 않습니다.");
            }
            return new UpdateEventCommand(title, categoryId, hostName, venueName, address, detailAddress,
                    kakaoPlaceId, legalDongCode, latitude, longitude, startDate, endDate);
        }
    }

}