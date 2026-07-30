package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerPricingPolicyRepository;
import com.coderhan.lastmission.marketing.domain.BannerPricingPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaBannerPricingPolicyRepository implements BannerPricingPolicyRepository {
    private final BannerPricingPolicyJpaRepository jpaRepository;

    @Override
    public BannerPricingPolicy save(UUID slotId, int durationDays, long price) {
        BannerPricingPolicyEntity entity = new BannerPricingPolicyEntity(
                null, slotId, durationDays, price, OffsetDateTime.now());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<BannerPricingPolicy> findBySlotId(UUID slotId) {
        return jpaRepository.findBySlotId(slotId).stream()
                .map(JpaBannerPricingPolicyRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<BannerPricingPolicy> findBySlotIdAndDurationDays(UUID slotId, int durationDays) {
        return jpaRepository.findBySlotIdAndDurationDays(slotId, durationDays)
                .map(JpaBannerPricingPolicyRepository::toDomain);
    }

    @Override
    @Transactional
    public void deleteBySlotId(UUID slotId) {
        jpaRepository.deleteBySlotId(slotId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static BannerPricingPolicy toDomain(BannerPricingPolicyEntity entity) {
        return new BannerPricingPolicy(entity.getId(), entity.getSlotId(),
                entity.getDurationDays(), entity.getPrice(), entity.getCreatedAt());
    }
}
