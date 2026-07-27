package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationOrderItemJpaRepository extends JpaRepository<ReservationOrderItemEntity, Long> {
    List<ReservationOrderItemEntity> findByOrderId(String orderId);
}
