package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 광고 슬롯 (Aggregate Root).
 * 관리자가 생성하며, 슬롯 타입(BANNER/TAB)에 따라 마케터가 첨부해야 하는 이미지가 달라진다.
 * 가격은 슬롯별 가격 정책(BannerPricingPolicy)에서 기간(일)에 따라 결정된다.
 */
public record BannerSlot(
        UUID id,
        String name,
        int maxCount,
        BannerSlotType type,
        OffsetDateTime createdAt
) {}
