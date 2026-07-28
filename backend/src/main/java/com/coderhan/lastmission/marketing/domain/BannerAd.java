package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 배너 광고 (Aggregate Root).
 * 마케터가 등록하며, 관리자 승인 후 노출 기간 내에 고객에게 노출된다.
 */
public record BannerAd(
        UUID id,
        UUID slotId,
        String title,
        String imageUrl,
        String linkUrl,
        int priority,
        BannerAdStatus status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String createdBy,
        OffsetDateTime createdAt
) {
    public static void validate(String title, String imageUrl, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 제목은 필수입니다.");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 이미지 URL은 필수입니다.");
        }
        if (startsAt == null || endsAt == null) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 기간은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }
    }
}
