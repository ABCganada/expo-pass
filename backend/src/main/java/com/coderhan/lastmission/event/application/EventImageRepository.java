package com.coderhan.lastmission.event.application;

import java.util.Optional;

import com.coderhan.lastmission.event.domain.EventImage;

public interface EventImageRepository {

    EventImage save(EventImage image);

    long countByEventId(long eventId);

    Optional<EventImage> findByIdAndEventId(long id, long eventId);

    void delete(EventImage image);
}