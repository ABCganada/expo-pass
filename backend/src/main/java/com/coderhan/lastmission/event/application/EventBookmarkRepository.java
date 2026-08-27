package com.coderhan.lastmission.event.application;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.domain.EventBookmark;

public interface EventBookmarkRepository {
    EventBookmark save(EventBookmark bookmark);

    void delete(EventBookmark bookmark);

    Optional<EventBookmark> findByEventIdAndUserId(long eventId, long userId);

    // 행사 삭제 시 해당 행사의 북마크 삭제
    void deleteAllByEventId(long eventId);

    // 내 북마크 목록 조회용. 최근 북마크한 순
    List<EventBookmark> findAllByUserIdOrderByCreatedAtDesc(long userId);
}