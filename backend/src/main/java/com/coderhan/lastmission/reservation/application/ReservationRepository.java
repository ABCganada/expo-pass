package com.coderhan.lastmission.reservation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;

public interface ReservationRepository {
    /** {@code ORD-yyyyMMdd-######} 형식의 새 주문 번호를 발급한다(시퀀스 기반). */
    String nextOrderId(LocalDate today);
    /** 새로운 예약(주문) 생성 */
    ReservationOrder createOrder(String orderId, long userId, long eventId, BigDecimal totalAmount,
            OffsetDateTime now);
    /** 티켓 한 장(row 하나)을 추가한다. 같은 티켓 여러 장은 서비스에서 이 메서드를 여러 번 호출한다. */
    ReservationOrderItem addItem(String orderId, long ticketId, BigDecimal unitPrice);
    /** 주문 찾기*/
    Optional<ReservationOrder> findOrder(String orderId);
    /** 티켓 정보 찾기*/
    List<ReservationOrderItem> findItems(String orderId);
}
