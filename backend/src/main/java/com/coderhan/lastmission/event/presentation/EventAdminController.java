package com.coderhan.lastmission.event.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.application.command.UpdateEventCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventAdminController {
    private final EventService eventService;

    @PatchMapping("/api/v1/admin/events/{eventId}")
    ApiResponse<UpdateEventResponse> updateEvent(@PathVariable long eventId,
            @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Event event = eventService.updateEvent(
                eventId, principal.userId(), isAdmin(authentication), request.toCommand());
        return ApiResponse.success(UpdateEventResponse.from(event));
    }

    @PatchMapping("/api/v1/admin/events/{eventId}/status")
    ApiResponse<UpdateEventResponse> changeStatus(@PathVariable long eventId,
            @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Event event = eventService.changeStatus(
                eventId, principal.userId(), isAdmin(authentication), request.toEventStatus());
        return ApiResponse.success(UpdateEventResponse.from(event));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    record UpdateEventRequest(
            String title, Long categoryId, String hostName, String venueName,
            String address, String detailAddress, String kakaoPlaceId, String legalDongCode,
            BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate
    ) {
        UpdateEventCommand toCommand() {
            if (categoryId == null || categoryId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "카테고리 id가 올바르지 않습니다.");
            }
            return new UpdateEventCommand(title, categoryId, hostName, venueName, address, detailAddress,
                    kakaoPlaceId, legalDongCode, latitude, longitude, startDate, endDate);
        }
    }

    record ChangeStatusRequest(String status) {
        EventStatus toEventStatus() {
            try {
                return EventStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException _) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "status 값이 올바르지 않습니다. (PUBLISHED, DRAFT, CANCELLED 중 하나여야 합니다.)");
            }
        }
    }

    record UpdateEventResponse(String id, String title, String categoryName, String managerId, EventStatus status) {
        static UpdateEventResponse from(Event event) {
            return new UpdateEventResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    event.getCategory().getName(),
                    Long.toString(event.getManagerId()),
                    event.getStatus());
        }
    }
}