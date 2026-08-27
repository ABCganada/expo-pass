package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SettlementJpaRepository extends JpaRepository<SettlementEntity, Long> {
    boolean existsByEventId(Long eventId);
    List<SettlementEntity> findByEventIdInOrderBySettledAtDesc(List<Long> eventIds);
    Optional<SettlementEntity> findByEventId(Long eventId);

    @Query(
        "SELECT COALESCE(SUM(s.totalSales), 0) " +
        "FROM SettlementEntity s"
    )
    BigDecimal sumTotalSales();

    @Query(
        "SELECT COALESCE(SUM(s.commissionAmount), 0) " +
        "FROM SettlementEntity s"
    )
    BigDecimal sumCommissionAmount();
}
