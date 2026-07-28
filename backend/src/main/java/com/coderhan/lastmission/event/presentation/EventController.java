package com.coderhan.lastmission.event.presentation;

import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.application.EventService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final EventService eventService;

    @GetMapping
    ApiResponse<List<EventListItemResponse>> list() {
        List<EventListItemResponse> events = eventService.getPublishedEvents()
                .stream()
                .map(EventListItemResponse::from)
                .toList();
        return ApiResponse.success(events);
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
}