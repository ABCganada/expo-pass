package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReservationOrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "qr_code_hash")
    private String qrCodeHash;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;

    @Column(name = "checked_in_by_admin_id")
    private Long checkedInByAdminId;

    ReservationOrderItemEntity(String orderId, Long ticketId, BigDecimal unitPrice, String qrCodeHash) {
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.unitPrice = unitPrice;
        this.qrCodeHash = qrCodeHash;
    }
}
