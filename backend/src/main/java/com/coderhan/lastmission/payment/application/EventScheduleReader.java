package com.coderhan.lastmission.payment.application;

import java.time.LocalDate;

/**
 * 환불율 판별을 위해 결제와 연결된 행사 시작일을 조회하는 포트.
 * 구현은 {@code infrastructure.schedule} 패키지 참고.
 */
public interface EventScheduleReader {
    LocalDate findEventStartDate(String orderId);
}
