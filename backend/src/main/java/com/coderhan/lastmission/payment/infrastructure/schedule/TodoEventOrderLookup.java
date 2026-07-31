package com.coderhan.lastmission.payment.infrastructure.schedule;

import java.util.List;
import com.coderhan.lastmission.payment.application.EventOrderLookup;
import org.springframework.stereotype.Component;

/**
 * {@link EventOrderLookup}의 임시 구현.
 *
 * TODO reservation 도메인과 실제 조회 연동으로 교체할 것. 그 전까지는 행사에 속한 주문을
 * 알 수 없으므로 빈 목록을 반환한다 — 존재하지 않는 매출을 만들어내는 것보다, 정산 매출이
 * 0원으로 계산되는 쪽이 안전하다.
 */
@Component
class TodoEventOrderLookup implements EventOrderLookup {

    @Override
    public List<String> findOrderIdsByEventId(long eventId) {
        return List.of();
    }
}
