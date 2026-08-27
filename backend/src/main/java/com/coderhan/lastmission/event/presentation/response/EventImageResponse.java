package com.coderhan.lastmission.event.presentation.response;

import java.time.Instant;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;

public record EventImageResponse(String id, String imageUrl, EventImageType imageType, int displayOrder, Instant createdAt) {
    public static EventImageResponse from(EventImage image) {
        return new EventImageResponse(Long.toString(image.getId()), image.getImageUrl(), image.getImageType(),
                image.getDisplayOrder(), image.getCreatedAt());
    }
}