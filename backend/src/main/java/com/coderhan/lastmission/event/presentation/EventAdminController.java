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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventAdminController {
    private final EventService eventService;

    @PostMapping("/api/v1/super-admin/events")
    ResponseEntity<ApiResponse<CreateEventResponse>> createDraftEvent(@RequestBody CreateEventRequest request) {
        long categoryId;
        long managerId;
        try {
            categoryId = Long.parseLong(request.categoryId());
            managerId = Long.parseLong(request.managerId());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "카테고리 또는 담당자 id가 올바르지 않습니다.");
        }

        if (categoryId <= 0 || managerId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "카테고리 또는 담당자 id가 올바르지 않습니다.");
        }

        Event event = eventService.createDraftEvent(request.title(), categoryId, managerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(CreateEventResponse.from(event)));
    }

    record CreateEventRequest(String title, String categoryId, String managerId) {}

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