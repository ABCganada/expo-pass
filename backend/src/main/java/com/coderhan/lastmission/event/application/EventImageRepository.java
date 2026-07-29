package com.coderhan.lastmission.event.application;

import com.coderhan.lastmission.event.domain.EventImage;

public interface EventImageRepository {

    EventImage save(EventImage image);

    long countByEventId(long eventId);
}