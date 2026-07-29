package com.coderhan.lastmission.payment.infrastructure.authorization;

import com.coderhan.lastmission.payment.application.EventManagerLookup;
import org.springframework.stereotype.Component;

/**
 * {@link EventManagerLookup}의 임시 구현.
 *
 * TODO reservation/event 도메인과 실제 조회 연동으로 교체할 것. 그 전까지는 담당 관리자를
 * 알 수 없으므로 항상 null을 반환한다 — MANAGER는 어떤 환불도 자기 것으로 인정받지 못해
 * 전부 접근 거부되고(안전한 기본값), ADMIN은 별도 분기로 이 조회 자체를 우회한다.
 */
@Component
class TodoEventManagerLookup implements EventManagerLookup {

    @Override
    public Long findEventManagerId(String orderId) {
        return null;
    }
}
