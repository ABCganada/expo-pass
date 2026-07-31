package com.coderhan.lastmission.payment.application;

import java.util.List;

/**
 * 정산 매출 집계를 위해, 특정 행사(eventId)에 속한 주문의 order_id 목록을 조회하는 포트.
 *
 * TODO 실제로는 reservation 모듈에 eventId로 order_id 목록을 조회하는 기능(예:
 * {@code ReservationOrderDirectory.findOrderIdsByEventId})을 요청해야 한다.
 * 협의가 끝나기 전까지는 임시 구현({@code infrastructure.schedule} 패키지)을 사용한다.
 */
public interface EventOrderLookup {
    List<String> findOrderIdsByEventId(long eventId);
}
