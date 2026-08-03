package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationOrderItemJpaRepository extends JpaRepository<ReservationOrderItemEntity, Long> {
    List<ReservationOrderItemEntity> findByOrderId(String orderId);
    Optional<ReservationOrderItemEntity> findByQrCodeHash(String qrCodeHash);

    // checked_in_at이 아직 NULL이고, 소속 주문이 CONFIRMED(결제완료)일 때만 성공 — 같은 QR이
    // 동시에 두 번 스캔돼도 하나만 처리되고, 환불/취소/결제대기 상태인 주문의 QR은 체크인 자체가
    // 안 된다(그렇지 않으면 환불된 결제로도 QR을 캡처해뒀다면 계속 입장할 수 있게 된다).
    @Modifying
    @Query("""
            UPDATE ReservationOrderItemEntity i
            SET i.checkedInAt = :now, i.checkedInByAdminId = :adminUserId
            WHERE i.qrCodeHash = :qrCodeHash AND i.checkedInAt IS NULL
            AND EXISTS (
                SELECT 1 FROM ReservationOrderEntity o
                WHERE o.orderId = i.orderId
                AND o.status = com.coderhan.lastmission.reservation.domain.OrderStatus.CONFIRMED
            )
            """)
    int checkin(@Param("qrCodeHash") String qrCodeHash, @Param("adminUserId") long adminUserId,
                @Param("now") OffsetDateTime now);

    // 이 유저가 이 티켓을 지금까지(모든 주문에 걸쳐) 총 몇 장 샀는지 센다 (1인당 구매 제한 검증용)
    @Query("SELECT COUNT(i) FROM ReservationOrderItemEntity i, ReservationOrderEntity o "
            + "WHERE i.orderId = o.orderId AND o.userId = :userId AND i.ticketId = :ticketId")
    long countByUserIdAndTicketId(@Param("userId") Long userId, @Param("ticketId") Long ticketId);

    // 주어진 주문들에 대해 주문ID·티켓ID별 수량을 한 번의 쿼리로 집계 (목록 화면 N+1 방지용 — 페이지 단위로만 조회)
    @Query("SELECT i.orderId as orderId, i.ticketId as ticketId, COUNT(i) as quantity "
            + "FROM ReservationOrderItemEntity i "
            + "WHERE i.orderId IN :orderIds "
            + "GROUP BY i.orderId, i.ticketId")
    List<OrderTicketQuantityRow> findTicketQuantitiesByOrderIds(@Param("orderIds") List<String> orderIds);

    interface OrderTicketQuantityRow {
        String getOrderId();
        Long getTicketId();
        long getQuantity();
    }

    // QR 자체는 주문 생성 시 바로 발급되지만(ReservationOrderItem 참고), 결제 확인 전엔 이 화면에
    // 노출하지 않는다 — 그래서 PENDING(결제대기)은 제외하고 CONFIRMED(결제완료)만 가져온다.
    @Query("SELECT i.orderId as orderId, o.eventId as eventId, i.orderItemId as orderItemId, "
            + "i.ticketId as ticketId, i.qrCodeHash as qrCodeHash, i.checkedInAt as checkedInAt "
            + "FROM ReservationOrderItemEntity i, ReservationOrderEntity o "
            + "WHERE i.orderId = o.orderId AND o.userId = :userId "
            + "AND o.status = com.coderhan.lastmission.reservation.domain.OrderStatus.CONFIRMED")
    List<QrTicketRow> findQrTicketsByUserId(@Param("userId") Long userId);

    interface QrTicketRow {
        String getOrderId();
        Long getEventId();
        Long getOrderItemId();
        Long getTicketId();
        String getQrCodeHash();
        OffsetDateTime getCheckedInAt();
    }

    // 이 행사의 CONFIRMED 주문에 속한 아이템 전체 수 / 체크인된 수를 한 번의 쿼리로 집계 (체크인 현황 화면용)
    @Query("SELECT COUNT(i) as totalItems, COUNT(i.checkedInAt) as checkedInCount "
            + "FROM ReservationOrderItemEntity i, ReservationOrderEntity o "
            + "WHERE i.orderId = o.orderId AND o.eventId = :eventId "
            + "AND o.status = com.coderhan.lastmission.reservation.domain.OrderStatus.CONFIRMED")
    CheckinProgressRow countCheckinProgressByEventId(@Param("eventId") long eventId);

    interface CheckinProgressRow {
        long getTotalItems();
        long getCheckedInCount();
    }

    // Event 도메인이 티켓을 삭제해도 되는지 확인할 때 사용 (PENDING/CONFIRMED만 유효한 예약으로 취급)
    @Query("SELECT COUNT(i) > 0 FROM ReservationOrderItemEntity i, ReservationOrderEntity o "
            + "WHERE i.orderId = o.orderId AND i.ticketId = :ticketId AND o.status IN "
            + "(com.coderhan.lastmission.reservation.domain.OrderStatus.PENDING, "
            + "com.coderhan.lastmission.reservation.domain.OrderStatus.CONFIRMED)")
    boolean existsActiveByTicketId(@Param("ticketId") long ticketId);
}
