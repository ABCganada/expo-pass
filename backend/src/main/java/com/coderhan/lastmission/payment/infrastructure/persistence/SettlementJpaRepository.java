package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementJpaRepository extends JpaRepository<SettlementEntity, Long> {
    boolean existsByEventId(Long eventId);
    List<SettlementEntity> findByEventIdInOrderBySettledAtDesc(List<Long> eventIds);
}
