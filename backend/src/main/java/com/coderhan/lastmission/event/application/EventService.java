package com.coderhan.lastmission.event.application;

import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DRAFT/CANCELLED는 고객에게 보이지 않는다 — PUBLISHED만 조회한다.
 * UPCOMING/ONGOING/ENDED는 저장되지 않으므로 조회 시점에 {@link Event#phase}로 계산해서 붙여준다.
 */
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventListItem> getPublishedEvents() {
        LocalDate today = LocalDate.now();
        return eventRepository.findByStatusOrderByStartDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(event -> new EventListItem(event, event.phase(today)))
                .toList();
    }

    public record EventListItem(Event event, EventPhase phase) {}
}