package com.coderhan.lastmission.event.presentation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import com.coderhan.lastmission.event.application.EventQueryService;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.application.command.UpdateEventCommand;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventManagerController {
    private final EventQueryService eventQueryService;
    private final EventService eventService;

    @GetMapping("/api/v1/manager/events")
    ApiResponse<List<AdminEventListItemResponse>> getAdminEvents(
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication,
            @RequestParam(required = false) String status) {
        List<AdminEventListItemResponse> events = eventQueryService
                .getAdminEvents(principal.userId(), isAdmin(authentication), parseStatus(status))
                .stream()
                .map(AdminEventListItemResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    @GetMapping("/api/v1/manager/events/{eventId}")
    ApiResponse<AdminEventDetailResponse> getEventDetail(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        EventQueryService.AdminEventDetailResult detail =
                eventQueryService.getAdminEventDetail(eventId, principal.userId(), isAdmin(authentication));
        return ApiResponse.success(AdminEventDetailResponse.from(detail));
    }

    @PatchMapping("/api/v1/manager/events/{eventId}")
    ApiResponse<UpdateEventResponse> updateEvent(@PathVariable long eventId,
            @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Event event = eventService.updateEvent(
                eventId, principal.userId(), isAdmin(authentication), request.toCommand());
        return ApiResponse.success(UpdateEventResponse.from(event));
    }

    @PatchMapping("/api/v1/manager/events/{eventId}/status")
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

    record AdminEventListItemResponse(
            String id, String title, String categoryName, String managerId,
            EventStatus status, LocalDate startDate, LocalDate endDate, EventPhase phase, long viewCount,
            String thumbnailUrl, Instant createdAt
    ) {
        static AdminEventListItemResponse from(EventQueryService.EventListItem eventItem) {
            Event event = eventItem.event();
            return new AdminEventListItemResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    event.getCategory().getName(),
                    Long.toString(event.getManagerId()),
                    event.getStatus(),
                    event.getStartDate(),
                    event.getEndDate(),
                    eventItem.phase(),
                    event.getViewCount(),
                    eventItem.thumbnailUrl(),
                    event.getCreatedAt());
        }
    }

    record AdminEventDetailResponse(
            String id, String title, String categoryName, String managerId, String managerName, String hostName,
            String venueName, String address, String detailAddress, String kakaoPlaceId,
            String legalDongCode, BigDecimal latitude, BigDecimal longitude,
            LocalDate startDate, LocalDate endDate, EventStatus status, EventPhase phase, long viewCount,
            Instant createdAt, Instant updatedAt,
            List<TicketResponse> tickets, List<EventContentResponse> contents, List<EventImageResponse> images
    ) {
        static AdminEventDetailResponse from(EventQueryService.AdminEventDetailResult detail) {
            Event event = detail.event();
            EventCategory category = event.getCategory();
            UserRef manager = detail.manager();
            return new AdminEventDetailResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    category.getName(),
                    Long.toString(event.getManagerId()),
                    manager == null ? null : manager.name(),
                    event.getHostName(),
                    event.getVenueName(),
                    event.getAddress(),
                    event.getDetailAddress(),
                    event.getKakaoPlaceId(),
                    event.getLegalDongCode(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getStartDate(),
                    event.getEndDate(),
                    event.getStatus(),
                    detail.phase(),
                    event.getViewCount(),
                    event.getCreatedAt(),
                    event.getUpdatedAt(),
                    detail.tickets().stream().map(TicketResponse::from).toList(),
                    detail.contents().stream().map(EventContentResponse::from).toList(),
                    detail.images().stream().map(EventImageResponse::from).toList());
        }
    }

    record TicketResponse(
            String id, String eventId, String name, int price, int quantityTotal, int quantityRemaining,
            int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt, Instant createdAt, Instant deletedAt
    ) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    Long.toString(ticket.getId()),
                    Long.toString(ticket.getEvent().getId()),
                    ticket.getName(),
                    ticket.getPrice(),
                    ticket.getQuantityTotal(),
                    ticket.getQuantityRemaining(),
                    ticket.getMaxPurchasePerUser(),
                    ticket.getSaleStartAt(),
                    ticket.getSaleEndAt(),
                    ticket.getCreatedAt(),
                    ticket.getDeletedAt());
        }
    }

    record EventContentResponse(String id, EventContentType contentType, String content, Instant updatedAt) {
        static EventContentResponse from(EventContent content) {
            return new EventContentResponse(
                    Long.toString(content.getId()), content.getContentType(), content.getContent(), content.getUpdatedAt());
        }
    }

    record EventImageResponse(String id, String imageUrl, EventImageType imageType, int displayOrder, Instant createdAt) {
        static EventImageResponse from(EventImage image) {
            return new EventImageResponse(Long.toString(image.getId()), image.getImageUrl(), image.getImageType(),
                    image.getDisplayOrder(), image.getCreatedAt());
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

    record ChangeStatusRequest(String status) {
        EventStatus toEventStatus() {
            try {
                return EventStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException _) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST,
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