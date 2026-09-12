package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 배너 광고 (Aggregate Root).
 * 광고주가 등록·결제(PENDING) 후 관리자가 승인하면 노출된다(APPROVED).
 * 슬롯은 복수 선택 가능하며, totalAmount는 등록 시점에 확정된다.
 */
public record BannerAd(
        UUID id,
        Set<UUID> slotIds,
        String orderId,         // 토스 결제 orderId (UUID 문자열)
        String title,
        String bannerImageUrl,  // BANNER 슬롯 선택 시 필요
        String adImageUrl,      // TAB 슬롯 선택 시 필요
        String linkUrl,
        BannerAdStatus status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        long createdBy,
        OffsetDateTime createdAt,
        Long totalAmount
) {
    public static void validate(String title, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 제목은 필수입니다.");
        }
        if (startsAt == null || endsAt == null) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 기간은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }
    }
}
