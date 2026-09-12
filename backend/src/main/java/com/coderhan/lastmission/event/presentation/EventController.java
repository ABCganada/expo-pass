package com.coderhan.lastmission.event.presentation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.application.EventQueryService;
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
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final EventQueryService eventQueryService;

    @GetMapping
    ApiResponse<List<EventListItemResponse>> getPublishedEvents(
            @RequestParam(required = false) Long categoryId) {
        List<EventListItemResponse> events = eventQueryService.getPublishedEvents(categoryId)
                .stream()
                .map(EventListItemResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    @GetMapping("/{eventId}")
    ApiResponse<EventDetailResponse> getEventDetail(@PathVariable long eventId, 
                                                    @AuthenticationPrincipal LastMissionPrincipal principal) {
        EventQueryService.EventDetail eventDetail = eventQueryService.getEventDetail(eventId, principal.userId());
        return ApiResponse.success(EventDetailResponse.from(eventDetail));
    }

    record EventListItemResponse(
            String id, String title, String categoryName, String venueName,
            LocalDate startDate, LocalDate endDate, EventPhase phase, long viewCount, String thumbnailUrl
    ) {
        static EventListItemResponse from(EventQueryService.EventListItem eventItem) {
            Event event = eventItem.event();
            EventCategory category = event.getCategory();
            return new EventListItemResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    category.getName(),
                    event.getVenueName(),
                    event.getStartDate(),
                    event.getEndDate(),
                    eventItem.phase(),
                    event.getViewCount(),
                    eventItem.thumbnailUrl());
        }
    }

    record EventDetailResponse(
            String id, String title, String categoryName, String hostName,
            String venueName, String address, String detailAddress, String kakaoPlaceId,
            String legalDongCode, BigDecimal latitude, BigDecimal longitude,
            LocalDate startDate, LocalDate endDate, EventStatus status, EventPhase phase, long viewCount,
            List<TicketSummary> tickets, List<EventContentSummary> contents, List<EventImageSummary> images
    ) {
        static EventDetailResponse from(EventQueryService.EventDetail detail) {
            Event event = detail.event();
            EventCategory category = event.getCategory();
            return new EventDetailResponse(
                    Long.toString(event.getId()),
                    event.getTitle(),
                    category.getName(),
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
                    detail.tickets().stream().map(TicketSummary::from).toList(),
                    detail.contents().stream().map(EventContentSummary::from).toList(),
                    detail.images().stream().map(EventImageSummary::from).toList());
        }
    }

    record TicketSummary(
            String id, String name, int price, int quantityRemaining,
            int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt
    ) {
        static TicketSummary from(Ticket ticket) {
            return new TicketSummary(
                    Long.toString(ticket.getId()),
                    ticket.getName(),
                    ticket.getPrice(),
                    ticket.getQuantityRemaining(),
                    ticket.getMaxPurchasePerUser(),
                    ticket.getSaleStartAt(),
                    ticket.getSaleEndAt());
        }
    }

    record EventContentSummary(String id, EventContentType contentType, String content, Instant updatedAt) {
        static EventContentSummary from(EventContent content) {
            return new EventContentSummary(
                    Long.toString(content.getId()), content.getContentType(), content.getContent(), content.getUpdatedAt());
        }
    }

    record EventImageSummary(String id, String imageUrl, EventImageType imageType, int displayOrder) {
        static EventImageSummary from(EventImage image) {
            return new EventImageSummary(
                    Long.toString(image.getId()), image.getImageUrl(), image.getImageType(), image.getDisplayOrder());
        }
    }
}