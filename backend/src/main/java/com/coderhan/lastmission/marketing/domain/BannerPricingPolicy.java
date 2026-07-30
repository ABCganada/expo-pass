package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 배너 슬롯 가격 정책.
 * 슬롯 × 기간(일) 조합으로 가격이 결정된다.
 */
public record BannerPricingPolicy(
        UUID id,
        UUID slotId,
        int durationDays,
        long price,
        OffsetDateTime createdAt
) {}
