package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.math.BigDecimal;
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

    ReservationOrderItemEntity(String orderId, Long ticketId, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.unitPrice = unitPrice;
    }
}
