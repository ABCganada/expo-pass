package com.coderhan.lastmission.event;

import java.util.Optional;

public interface EventManagerQueryPort {

    /** 이 행사의 담당 매니저 ID 조회 */
    Optional<Long> findEventManagerId(long eventId);
}
