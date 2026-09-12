package com.coderhan.lastmission.event.presentation;

import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventAdminController {
    private final EventService eventService;

    @PostMapping("/api/v1/admin/events")
    ResponseEntity<ApiResponse<EventSummaryResponse>> createDraftEvent(@RequestBody CreateEventRequest request) {
        Event event = eventService.createDraftEvent(
                request.title(), request.validateCategoryId(), request.validateManagerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(EventSummaryResponse.from(event)));
    }

    @DeleteMapping("/api/v1/admin/events/{eventId}")
    ApiResponse<Void> deleteEvent(@PathVariable long eventId) {
        eventService.deleteEvent(eventId);
        return ApiResponse.success("행사를 삭제했습니다.", null);
    }

    @PatchMapping("/api/v1/admin/events/{eventId}/manager")
    ApiResponse<EventSummaryResponse> changeManager(@PathVariable long eventId, @RequestBody ChangeManagerRequest request) {
        Event event = eventService.changeManager(eventId, request.managerId());
        return ApiResponse.success("담당자를 변경했습니다.", EventSummaryResponse.from(event));
    }

    record ChangeManagerRequest(Long managerId) {
        ChangeManagerRequest {
            if (managerId == null || managerId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "담당자 id가 올바르지 않습니다.");
            }
        }
    }

    record CreateEventRequest(String title, Long categoryId, Long managerId) {
        long validateCategoryId() {
            if (categoryId == null || categoryId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "카테고리 id가 올바르지 않습니다.");
            }
            return categoryId;
        }

        long validateManagerId() {
            if (managerId == null || managerId <= 0) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "담당자 id가 올바르지 않습니다.");
            }
            return managerId;
        }
    }

    record EventSummaryResponse(String id, String title, String categoryName, String managerId, EventStatus status) {
        static EventSummaryResponse from(Event event) {
            return new EventSummaryResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    event.getCategory().getName(),
                    Long.toString(event.getManagerId()),
                    event.getStatus());
        }
    }
}