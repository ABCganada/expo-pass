package com.coderhan.lastmission.reservation.domain;

import java.time.OffsetDateTime;

/** 입장 체크인 이력 한 건. Day 3(QR 체크인)에서 저장소/서비스가 연결된다. */
public record ReservationCheckin(
        long checkinId,
        long orderItemId,
        OffsetDateTime checkedInAt,
        long adminUserId,
        CheckinStatus status
) {
}
