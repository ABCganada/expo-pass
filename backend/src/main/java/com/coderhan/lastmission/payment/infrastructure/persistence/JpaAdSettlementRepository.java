package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.payment.application.AdSettlementRepository;
import com.coderhan.lastmission.payment.domain.AdSettlement;
import com.coderhan.lastmission.payment.domain.AdSettlementSummary;
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
            BigDecimal netAmount,
            OffsetDateTime settledAt
    ) {
        AdSettlementEntity saved = jpaRepository.save(
                AdSettlementEntity.completed(adId, totalAmount, netAmount, settledAt));

        return toDomain(saved);
    }

    @Override
    public List<AdSettlement> findAll() {
        return jpaRepository.findAllByOrderBySettledAtDesc().stream()
                .map(JpaAdSettlementRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<AdSettlement> findById(long adSettlementId) {
        return jpaRepository.findById(adSettlementId)
                .map(JpaAdSettlementRepository::toDomain);
    }

    @Override
    public AdSettlementSummary getDashboardSummary() {
        return new AdSettlementSummary(
                jpaRepository.sumTotalAmount(),
                jpaRepository.sumNetAmount(),
                jpaRepository.count());
    }

    private static AdSettlement toDomain(AdSettlementEntity entity) {
        return AdSettlement.builder()
                .id(entity.getId())
                .adId(entity.getAdId())
                .totalAmount(entity.getTotalAmount())
                .netAmount(entity.getNetAmount())
                .status(entity.getStatus())
                .settledAt(entity.getSettledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
