package com.coderhan.lastmission.event.presentation.response;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;

/** 행사 생성/수정/상태변경/담당자변경 등 쓰기 액션의 공통 응답 */
public record EventSummaryResponse(String id, String title, String categoryName, String managerId, EventStatus status) {
    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                Long.toString(event.getId()),
                event.getTitle(),
                event.getCategory().getName(),
                Long.toString(event.getManagerId()),
                event.getStatus());
    }
}