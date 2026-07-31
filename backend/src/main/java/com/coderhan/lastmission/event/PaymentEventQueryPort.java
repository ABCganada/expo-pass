package com.coderhan.lastmission.event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * payment 도메인이 필요로 하는 행사 관련 조회 전용 포트.
 */
public interface PaymentEventQueryPort {

    /** 이 행사를 담당하는 매니저 id */
    Optional<Long> findEventManagerId(long eventId);

    /** 이 매니저가 담당하는(삭제되지 않은) 행사 id 목록. */
    List<Long> findEventIdsManagedBy(long managerId);

    /** 행사 시작일 */
    Optional<LocalDate> findEventStartDate(long eventId);
}