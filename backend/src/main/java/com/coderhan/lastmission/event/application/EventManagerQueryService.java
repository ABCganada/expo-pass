package com.coderhan.lastmission.event.application;

import com.coderhan.lastmission.event.EventManagerQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventManagerQueryService implements EventManagerQueryPort {

    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findEventManagerId(long eventId) {
        return eventRepository.findManagerIdById(eventId);
    }
}
