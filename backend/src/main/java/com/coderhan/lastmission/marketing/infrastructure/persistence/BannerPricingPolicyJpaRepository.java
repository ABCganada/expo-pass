package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BannerPricingPolicyJpaRepository extends JpaRepository<BannerPricingPolicyEntity, UUID> {
    List<BannerPricingPolicyEntity> findBySlotId(UUID slotId);
    Optional<BannerPricingPolicyEntity> findBySlotIdAndDurationDays(UUID slotId, int durationDays);
    void deleteBySlotId(UUID slotId);
}
