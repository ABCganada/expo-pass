package com.coderhan.lastmission.event;

import java.util.List;
import java.util.Optional;

public interface EventManagerQueryPort {

    /** 이 행사를 담당하는 매니저 id */
    Optional<Long> findEventManagerId(long eventId);

    /** 이 매니저가 담당하는(삭제되지 않은) 행사 id 목록. */
    List<Long> findEventIdsManagedBy(long managerId);
}