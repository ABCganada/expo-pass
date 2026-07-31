package com.coderhan.lastmission.reservation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 다른 도메인(Payment 등)이 주문 금액을 조회하기 위한 공개 인터페이스.
 * reservation 루트 패키지에 있어서, allowedDependencies에 "reservation"을 추가한
 * 모듈이면 별도 @NamedInterface 없이 바로 주입받아 쓸 수 있다 (user.UserDirectory와 동일한 패턴).
 */
public interface ReservationOrderDirectory {
    /** orderId에 해당하는 주문의 총액을 조회한다. 존재하지 않는 주문이면 빈 Optional. */
    Optional<BigDecimal> findOrderAmount(String orderId);
    /**
     * 이 행사에 속한 모든 주문 ID를 조회한다(Payment 도메인이 정산 대상 매출을 계산할 때 사용).
     * 상태(PENDING/CONFIRMED/CANCELLED/REFUNDED) 구분 없이 전부 반환한다 — 실제 결제 여부는
     * Payment 자신의 결제 기록으로 판단해야 하므로, 여기서는 필터링하지 않는다.
     */
    List<String> findOrderIdsByEventId(long eventId);
}
