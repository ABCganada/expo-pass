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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventSuperAdminController {
    private final EventService eventService;

    @PostMapping("/api/v1/super-admin/events")
    ResponseEntity<ApiResponse<CreateEventResponse>> createDraftEvent(@RequestBody CreateEventRequest request) {
        Event event = eventService.createDraftEvent(
                request.title(), request.requireCategoryId(), request.requireManagerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CreateEventResponse.from(event)));
    }

    @DeleteMapping("/api/v1/super-admin/events/{eventId}")
    ApiResponse<Void> deleteEvent(@PathVariable long eventId) {
        eventService.deleteEvent(eventId);
        return ApiResponse.success("행사를 삭제했습니다.", null);
    }

    record CreateEventRequest(String title, Long categoryId, Long managerId) {
        long requireCategoryId() {
            if (categoryId == null || categoryId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "카테고리 id가 올바르지 않습니다.");
            }
            return categoryId;
        }

        long requireManagerId() {
            if (managerId == null || managerId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "담당자 id가 올바르지 않습니다.");
            }
            return managerId;
        }
    }

    record CreateEventResponse(String id, String title, String categoryName, String managerId, EventStatus status) {
        static CreateEventResponse from(Event event) {
            return new CreateEventResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    event.getCategory().getName(),
                    Long.toString(event.getManagerId()),
                    event.getStatus());
        }
    }
}