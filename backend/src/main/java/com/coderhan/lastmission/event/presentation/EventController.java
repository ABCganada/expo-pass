package com.coderhan.lastmission.event.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final EventService eventService;

    @GetMapping
    ApiResponse<List<EventListItemResponse>> getPublishedEvents() {
        List<EventListItemResponse> events = eventService.getPublishedEvents()
                .stream()
                .map(EventListItemResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    @GetMapping("/{eventId}")
    ApiResponse<EventDetailResponse> getEventDetail(@PathVariable long eventId) {
        EventService.EventDetail eventDetail = eventService.getEventDetail(eventId);
        return ApiResponse.success(EventDetailResponse.from(eventDetail));
    }

    record EventListItemResponse(
            String id, String title, String categoryName, String venueName,
            LocalDate startDate, LocalDate endDate, EventPhase phase, long viewCount
    ) {
        static EventListItemResponse from(EventService.EventListItem eventItem) {
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
                    event.getViewCount());
        }
    }

    record EventDetailResponse(
            String id, String title, String categoryName, String hostName,
            String venueName, String address, String detailAddress, String kakaoPlaceId,
            String legalDongCode, BigDecimal latitude, BigDecimal longitude,
            LocalDate startDate, LocalDate endDate, EventStatus status, EventPhase phase, long viewCount
    ) {
        static EventDetailResponse from(EventService.EventDetail detail) {
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
                    event.getViewCount());
        }
    }
}