package com.coderhan.lastmission.event.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;

public interface EventImageRepository {

    EventImage save(EventImage image);

    long countByEventId(long eventId);

    Optional<EventImage> findByIdAndEventId(long id, long eventId);

    void delete(EventImage image);

    /** 행사 상세 조회용 - 행사의 전체 이미지 */
    List<EventImage> findAllByEventIdOrderByDisplayOrderAsc(long eventId);

    /** 행사 목록 조회용 - 여러 행사의 썸네일만 배치로 조회 (N+1 방지) */
    List<EventImage> findAllByEventIdInAndImageType(Collection<Long> eventIds, EventImageType imageType);
}