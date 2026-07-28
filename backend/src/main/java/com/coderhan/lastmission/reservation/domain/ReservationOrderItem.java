package com.coderhan.lastmission.reservation.domain;

import java.math.BigDecimal;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 주문에 속한 티켓 항목 한 건 — 티켓 한 장(row 하나 = 티켓 한 장)을 의미한다.
 *
 * <p>같은 티켓을 여러 장 사면, row 가 여러 개 생긴다. 각 장마다 현장에서
 * 독립적으로 QR 체크인이 되어야 하므로, {@code quantity} 는 이 record 에 저장하지 않는다
 * (요청 시점에만 존재 — {@link #validate}, {@code ReservationService} 참고).</p>
 *
 * <p>{@code qrCodeHash} 는 결제 완료 시 채워진다.</p>
 */
public record ReservationOrderItem(
        long orderItemId,
        String orderId,
        long ticketId,
        BigDecimal unitPrice,
        String qrCodeHash
) {
    /** 요청으로 들어온 항목(티켓 종류+수량)이 유효한지 확인한다. 저장 전 서비스에서 호출한다. */
    public static void validate(long ticketId, BigDecimal unitPrice, int quantity) {
        if (ticketId <= 0) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "티켓 ID가 올바르지 않습니다.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "단가가 올바르지 않습니다.");
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
    }
}
