package com.coderhan.lastmission.event.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventBookmark;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventBookmarkService {
    private final EventBookmarkRepository eventBookmarkRepository;
    private final EventRepository eventRepository;
    private final Clock clock;

    /**
     * 행사 찜 토글
     */
    @Transactional
    public boolean toggleBookmark(long eventId, long userId) {
        Optional<EventBookmark> existing = eventBookmarkRepository.findByEventIdAndUserId(eventId, userId);

        if (existing.isPresent()) {
            eventBookmarkRepository.delete(existing.get());
            return false;
        }

        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        eventBookmarkRepository.save(new EventBookmark(event, userId));
        return true;
    }

    /**
     * 행사 삭제 시 그 행사의 북마크를 함께 삭제
     */
    @Transactional
    public void removeBookmarksForEvent(long eventId) {
        eventBookmarkRepository.deleteAllByEventId(eventId);
    }

    /**
     * 내 북마크 목록 조회 - 최근 북마크한 순
     */
    @Transactional(readOnly = true)
    public List<BookmarkedEvent> getMyBookmarkedEvents(long userId) {
        LocalDate today = LocalDate.now(clock);
        return eventBookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bookmark -> BookmarkedEvent.from(bookmark, today))
                .toList();
    }

    public record BookmarkedEvent(Event event, EventPhase phase) {
        static BookmarkedEvent from(EventBookmark bookmark, LocalDate today) {
            Event event = bookmark.getEvent();
            return new BookmarkedEvent(event, event.phase(today));
        }
    }
}