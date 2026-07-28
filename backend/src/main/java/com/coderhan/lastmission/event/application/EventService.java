package com.coderhan.lastmission.event.application;

import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.event.domain.EventStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 목록 조회 - PUBLISHED만 조회
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

    /**
     * 행사 상세 조회
     */
    @Transactional(readOnly = true)
    public EventDetail getEventDetail(long id) {
        Event event = eventRepository.findNotDeletedById(id)
                .filter(candidate -> candidate.getStatus() != EventStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        return new EventDetail(event, event.phase(LocalDate.now()));
    }

    public record EventListItem(Event event, EventPhase phase) {}

    public record EventDetail(Event event, EventPhase phase) {}
}