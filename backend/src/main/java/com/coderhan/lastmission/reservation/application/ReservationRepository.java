package com.coderhan.lastmission.reservation.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.coderhan.lastmission.reservation.domain.*;

public interface ReservationRepository {
    /** {@code ORD-yyyyMMdd-######} 형식의 새 주문 번호를 발급한다(시퀀스 기반). */
    String nextOrderId(LocalDate today);
    /** 새로운 예약(주문) 생성 */
    ReservationOrder createOrder(String orderId, long userId, long eventId, BigDecimal totalAmount,
            OffsetDateTime now);
    /** 티켓 한 장(row 하나)을 추가한다. 같은 티켓 여러 장은 서비스에서 이 메서드를 여러 번 호출한다. */
    ReservationOrderItem addItem(String orderId, long ticketId, BigDecimal unitPrice, String qrCodeHash);
    /** 주문 찾기*/
    Optional<ReservationOrder> findOrder(String orderId);
    /** 티켓 정보 찾기*/
    List<ReservationOrderItem> findItems(String orderId);
    /** QR 값으로 티켓 한 장을 찾는다. 체크인 시 사용. */
    Optional<ReservationOrderItem> findItemByQrCodeHash(String qrCodeHash);
    /** 체크인 처리(조건부 UPDATE). 이미 체크인됐거나 존재하지 않는 QR이면 false. */
    boolean checkin(String qrCodeHash, long adminUserId, OffsetDateTime now);
    /** 이 유저의 모든 주문을 최신순으로 찾는다. */
    List<ReservationOrder> findOrdersByUserId(long userId);
    /** 이 행사의 모든 주문을 최신순으로 찾는다(관리자 예약자 명단용). */
    List<ReservationOrder> findOrdersByEventId(long eventId);
    /** 이 행사의 주문을 상태별로 집계한다(관리자 예약 현황용). */
    Map<OrderStatus, Long> countOrdersByEventIdGroupedByStatus(long eventId);
    /** 이 유저가 이 티켓을 지금까지 총 몇 장 샀는지(1인당 구매 제한 검증용). */
    long countPurchasedQuantity(long userId, long ticketId);
    /**
     * 이 유저의 모든 주문에 대해, 주문ID별 티켓 종류별 수량을 한 번에 집계한다(목록 화면에서
     * 주문마다 상세를 따로 조회하는 N+1을 피하기 위한 일괄 조회).
     */
    Map<String, List<TicketQuantity>> findTicketQuantitiesByUserId(long userId);
    /** 이 유저의 QR 발급 대상(취소/환불 제외) 티켓을 전부 한 번에 조회한다(QR 화면 N+1 방지용). */
    List<QrTicketView> findQrTicketsByUserId(long userId);
    /** 해당 이벤트의 현재 체크인 현황 조회. */
    CheckinProgress countCheckinProgressByEventId(long eventId);
}
