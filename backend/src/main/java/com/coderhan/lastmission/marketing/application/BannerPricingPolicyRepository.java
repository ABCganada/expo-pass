package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerPricingPolicy;

public interface BannerPricingPolicyRepository {
    BannerPricingPolicy save(UUID slotId, int durationDays, long price);
    List<BannerPricingPolicy> findBySlotId(UUID slotId);
    Optional<BannerPricingPolicy> findBySlotIdAndDurationDays(UUID slotId, int durationDays);
    void deleteBySlotId(UUID slotId);
    void deleteById(UUID id);
}
