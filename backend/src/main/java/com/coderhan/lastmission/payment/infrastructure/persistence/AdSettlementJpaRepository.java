package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AdSettlementJpaRepository extends JpaRepository<AdSettlementEntity, Long> {
    boolean existsByAdId(UUID adId);
    List<AdSettlementEntity> findAllByOrderBySettledAtDesc();

    @Query(
        "SELECT COALESCE(SUM(s.totalAmount), 0) " +
        "FROM AdSettlementEntity s"
    )
    BigDecimal sumTotalAmount();

    @Query(
        "SELECT COALESCE(SUM(s.netAmount), 0) " +
        "FROM AdSettlementEntity s"
    )
    BigDecimal sumNetAmount();
}
