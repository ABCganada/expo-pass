package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.SettlementRepository;
import com.coderhan.lastmission.payment.domain.Settlement;
import com.coderhan.lastmission.payment.domain.SettlementSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaSettlementRepository implements SettlementRepository {

    private final SettlementJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(long eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public Settlement save(long eventId, BigDecimal totalSales, BigDecimal commissionRate,
                           BigDecimal commissionAmount, BigDecimal netAmount, OffsetDateTime settledAt) {
        SettlementEntity saved = jpaRepository.save(
                SettlementEntity.completed(eventId, totalSales, commissionRate, commissionAmount, netAmount, settledAt));

        return toDomain(saved);
    }

    @Override
    public List<Settlement> findByEventIdIn(List<Long> eventIds) {
        return jpaRepository.findByEventIdInOrderBySettledAtDesc(eventIds).stream()
                .map(JpaSettlementRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<Settlement> findById(long settlementId) {
        return jpaRepository.findById(settlementId)
                .map(JpaSettlementRepository::toDomain);
    }

    @Override
    public SettlementSummary getDashboardSummary() {
        return new SettlementSummary(
                jpaRepository.sumTotalSales(),
                jpaRepository.sumCommissionAmount(),
                jpaRepository.count());
    }

    private static Settlement toDomain(SettlementEntity entity) {
        return Settlement.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .totalSales(entity.getTotalSales())
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
