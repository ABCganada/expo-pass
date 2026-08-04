package com.coderhan.lastmission.event.presentation.response;

import java.time.Instant;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;

public record EventContentResponse(String id, EventContentType contentType, String content, Instant updatedAt) {
    public static EventContentResponse from(EventContent content) {
        return new EventContentResponse(
                Long.toString(content.getId()), content.getContentType(), content.getContent(), content.getUpdatedAt());
    }
}