package com.coderhan.lastmission.event.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventStatus;

/**
 * <p>모든 조회 메서드는 소프트 삭제된 행사({@code deleted_at IS NOT NULL})를 제외한다.</p>
 */
public interface EventRepository {

    Event save(Event event);

    Optional<Event> findNotDeletedById(long id);

    // 고객 목록 조회용. status + start_date 순으로 정렬
    List<Event> findByStatusOrderByStartDateAsc(EventStatus status);

    // 고객 목록 조회용(카테고리 필터). status + category_id + start_date 순으로 정렬
    List<Event> findByStatusAndCategoryIdOrderByStartDateAsc(EventStatus status, long categoryId);

    // 관리자 목록 조회용. start_date 순으로 정렬
    List<Event> findAllOrderByStartDateAsc();

    // 행사 종료 감지 스케줄러용
    List<Event> findEndedEventsNotNotified(LocalDate date);

    // PaymentEventQueryPort용
    List<Long> findIdsByManagerId(long managerId);

    // PaymentEventQueryPort용
    Optional<LocalDate> findStartDateByEventId(long eventId);
}