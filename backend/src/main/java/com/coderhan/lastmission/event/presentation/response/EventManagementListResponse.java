package com.coderhan.lastmission.event.presentation.response;

import java.time.Instant;
import java.time.LocalDate;
import com.coderhan.lastmission.event.application.EventQueryService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;

/** 매니저용/관리자용 행사 목록 조회 응답 */
public record EventManagementListResponse(
        String id, String title, String categoryName, String managerId,
        EventStatus status, LocalDate startDate, LocalDate endDate, EventPhase phase, long viewCount,
        String thumbnailUrl, Instant createdAt
) {
    public static EventManagementListResponse from(EventQueryService.EventListItem eventItem) {
        Event event = eventItem.event();
        return new EventManagementListResponse(
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