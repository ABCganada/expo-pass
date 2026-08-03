package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.coderhan.lastmission.payment.application.AdSettlementRepository;
import com.coderhan.lastmission.payment.domain.AdSettlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaAdSettlementRepository implements AdSettlementRepository {

    private final AdSettlementJpaRepository jpaRepository;

    @Override
    public boolean existsByAdId(UUID adId) {
        return jpaRepository.existsByAdId(adId);
    }

    @Override
    public AdSettlement save(
            UUID adId,
            BigDecimal totalAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal netAmount,
            OffsetDateTime settledAt
    ) {
        AdSettlementEntity saved = jpaRepository.save(
                AdSettlementEntity.completed(adId, totalAmount, commissionRate, commissionAmount, netAmount, settledAt));

        return toDomain(saved);
    }

    private static AdSettlement toDomain(AdSettlementEntity entity) {
        return AdSettlement.builder()
                .id(entity.getId())
                .adId(entity.getAdId())
                .totalAmount(entity.getTotalAmount())
                .commissionRate(entity.getCommissionRate())
                .commissionAmount(entity.getCommissionAmount())
                .netAmount(entity.getNetAmount())
                .status(entity.getStatus())
                .settledAt(entity.getSettledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
