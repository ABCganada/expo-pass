package com.coderhan.lastmission.shared.order;

import java.util.Collection;
import java.util.List;

/**
 * 다른 도메인(Reservation/Marketing)이 주문의 결제 완료 여부를 조회하기 위한 공개 인터페이스.
 *
 * PENDING 상태로 오래 남은 주문을 정리하는 정합성 스케줄링은 각 도메인(reservation/marketing)이
 * 자기 테이블에 대해 직접 수행한다 — payment는 그 판단에 필요한 결제 완료 여부만 읽기 전용으로 제공한다.
 */
public interface PaymentOrderDirectory {
    /**
     * 넘겨받은 orderId 중 실제 COMPLETED 상태 결제가 존재하는 것만 골라 반환.
     * 존재하지 않거나 COMPLETED가 아닌 orderId는 결과에서 빠진다(호출부는 "빠진 것 = 아직 결제 안 됨"으로 판단).
     */
    List<String> findCompletedOrderIds(Collection<String> orderIds);
}
