package com.coderhan.lastmission.marketing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 광고 슬롯. BANNER / TAB 두 종류가 고정으로 존재하며 관리자가 추가·삭제하지 않는다.
 * 가격은 pricePerDay × 광고 기간(일)으로 자동 산정된다.
 */
public record BannerSlot(
        UUID id,
        String name,
        int maxCount,
        BannerSlotType type,
        long pricePerDay,
        OffsetDateTime createdAt
) {}
