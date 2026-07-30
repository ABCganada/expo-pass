package com.coderhan.lastmission.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementJpaRepository extends JpaRepository<SettlementEntity, Long> {
    boolean existsByEventId(Long eventId);
}
