package com.coderhan.lastmission.payment.application;

import java.time.LocalDate;

/**
 * 환불 자동승인(D-3) 판별을 위해 결제와 연결된 행사 시작일을 조회하는 포트.
 *
 * TODO 실제로는 reservation 모듈에서 order_id로 eventId를 조회하고, event 모듈에서
 * eventId로 행사 시작일을 조회해야 한다. 두 도메인 담당자와 인터페이스(named interface)
 * 협의가 끝나기 전까지는 임시 구현({@code infrastructure.schedule} 패키지)을 사용한다.
 */
public interface EventScheduleReader {
    LocalDate findEventStartDate(String orderId);
}
