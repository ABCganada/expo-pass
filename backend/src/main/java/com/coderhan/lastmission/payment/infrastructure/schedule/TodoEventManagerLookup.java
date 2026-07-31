package com.coderhan.lastmission.payment.infrastructure.schedule;

import java.util.List;
import com.coderhan.lastmission.payment.application.EventManagerLookup;
import org.springframework.stereotype.Component;

/**
 * {@link EventManagerLookup}의 임시 구현.
 *
 * TODO event 도메인과 실제 조회 연동으로 교체할 것. 그 전까지는 담당 관리자를 알 수 없으므로
 * 항상 null/빈 목록을 반환한다 — MANAGER는 어떤 정산도 자기 것으로 인정받지 못해 전부 접근
 * 거부되는 안전한 기본값이다.
 */
@Component
class TodoEventManagerLookup implements EventManagerLookup {

    @Override
    public Long findEventManagerId(long eventId) {
        return null;
    }

    @Override
    public List<Long> findEventIdsManagedBy(long managerId) {
        return List.of();
    }
}
