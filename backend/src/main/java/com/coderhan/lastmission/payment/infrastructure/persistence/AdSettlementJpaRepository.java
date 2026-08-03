package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdSettlementJpaRepository extends JpaRepository<AdSettlementEntity, Long> {
    boolean existsByAdId(UUID adId);
}
