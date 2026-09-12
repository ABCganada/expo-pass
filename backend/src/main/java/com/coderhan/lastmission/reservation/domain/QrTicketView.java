package com.coderhan.lastmission.reservation.domain;

import java.time.OffsetDateTime;

/** QR 티켓 화면용 조회 전용 뷰 — 주문(orders)과 아이템(order_items)을 조인한 결과 한 줄. */
public record QrTicketView(
        String orderId, long eventId, long orderItemId, long ticketId, String qrCodeHash,
        OffsetDateTime checkedInAt
) {}
