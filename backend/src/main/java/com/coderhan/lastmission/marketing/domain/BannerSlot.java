package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 광고 슬롯 (Aggregate Root).
 * 관리자가 생성하며, 슬롯당 최대 광고 수(maxCount)를 지정한다.
 */
public record BannerSlot(
        UUID id,
        String name,
        int maxCount,
        OffsetDateTime createdAt
) {}
