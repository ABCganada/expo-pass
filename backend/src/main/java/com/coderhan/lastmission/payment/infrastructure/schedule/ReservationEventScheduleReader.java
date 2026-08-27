package com.coderhan.lastmission.payment.infrastructure.schedule;

import java.time.LocalDate;
import com.coderhan.lastmission.event.PaymentEventQueryPort;
import com.coderhan.lastmission.payment.application.EventScheduleReader;
import com.coderhan.lastmission.reservation.ReservationOrderDirectory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link EventScheduleReader}의 실제 구현. orderId로 예약 주문을 찾아 eventId를 얻고,
 * 그 eventId로 행사 시작일을 조회한다. 환불은 예약 주문에 대해서만 신청 가능하므로
 * (광고 주문 환불은 시나리오에 없음) 예약 주문을 찾지 못하면 환불 자체를 거부한다.
 */
@Component
@RequiredArgsConstructor
class ReservationEventScheduleReader implements EventScheduleReader {

    private final ReservationOrderDirectory reservationOrderDirectory;
    private final PaymentEventQueryPort paymentEventQueryPort;

    @Override
    public LocalDate findEventStartDate(String orderId) {
        long eventId = reservationOrderDirectory.findEventIdByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED, "환불 가능한 예약 주문을 찾을 수 없습니다."));

        return paymentEventQueryPort.findEventStartDate(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED, "행사 시작일을 확인할 수 없습니다."));
    }
}
