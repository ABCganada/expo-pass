package com.coderhan.lastmission.event.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.PaymentEventQueryPort;
import com.coderhan.lastmission.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PaymentEventQueryService implements PaymentEventQueryPort {
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findEventManagerId(long eventId) {
        return eventRepository.findNotDeletedById(eventId).map(Event::getManagerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findEventIdsManagedBy(long managerId) {
        return eventRepository.findIdsByManagerId(managerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> findEventStartDate(long eventId) {
        return eventRepository.findStartDateByEventId(eventId);
    }
}