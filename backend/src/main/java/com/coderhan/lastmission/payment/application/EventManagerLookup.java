package com.coderhan.lastmission.payment.application;

/**
 * 환불 승인/거절 시 "본인이 담당하는 행사인지" 검사하기 위해, 주문(orderId)이 속한 행사의
 * 담당 관리자 userId를 조회하는 포트.
 *
 * TODO 실제로는 reservation 모듈에서 order_id로 eventId를 조회하고, event 모듈에서
 * eventId로 Event.managerId를 조회해야 한다. 두 도메인 담당자와 인터페이스(named interface)
 * 협의가 끝나기 전까지는 임시 구현({@code infrastructure.authorization} 패키지)을 사용한다.
 */
public interface EventManagerLookup {
    /** 알 수 없으면 null — 어떤 MANAGER와도 일치하지 않으므로 안전하게 접근이 거부된다. */
    Long findEventManagerId(String orderId);
}
