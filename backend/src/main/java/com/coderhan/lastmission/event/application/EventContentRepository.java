package com.coderhan.lastmission.event.application;

import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;

public interface EventContentRepository {

    EventContent save(EventContent content);

    Optional<EventContent> findByEventIdAndContentType(long eventId, EventContentType contentType);

    List<EventContent> findAllByEventId(long eventId);
}