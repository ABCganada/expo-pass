package com.coderhan.lastmission.event.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.application.EventQueryService;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.user.UserRef;

/** 매니저용/관리자용 행사 상세 조회 응답 */
public record EventManagementDetailResponse(
        String id, String title, String categoryName, String managerId, String managerName, String hostName,
        String venueName, String address, String detailAddress, String kakaoPlaceId,
        String legalDongCode, BigDecimal latitude, BigDecimal longitude,
        LocalDate startDate, LocalDate endDate, EventStatus status, EventPhase phase, long viewCount,
        Instant createdAt, Instant updatedAt,
        List<TicketResponse> tickets, List<EventContentResponse> contents, List<EventImageResponse> images
) {
    public static EventManagementDetailResponse from(EventQueryService.EventManagementDetail detail) {
        Event event = detail.event();
        EventCategory category = event.getCategory();
        UserRef manager = detail.manager();
        return new EventManagementDetailResponse(
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