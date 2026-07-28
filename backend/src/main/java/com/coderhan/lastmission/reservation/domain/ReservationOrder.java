package com.coderhan.lastmission.reservation.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 박람회 티켓 주문 한 건 (Aggregate Root).
 *
 * <p>Day 1 범위에서는 Event 도메인이 없어 {@code eventId}/티켓 가격을 그대로 신뢰한다.
 * Day 4에서 Event 도메인 연동 시 실제 검증으로 교체한다.</p>
 */
public record ReservationOrder(
        String orderId,
        long userId,
        long eventId,
        OrderStatus status,
        BigDecimal totalAmount,
        OffsetDateTime reservedAt,
        OffsetDateTime updatedAt
) {
    public static void validateEventId(long eventId) {
        if (eventId <= 0) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "행사 ID가 올바르지 않습니다.");
        }
    }
}
