package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationOrderJpaRepository extends JpaRepository<ReservationOrderEntity, String> {
    List<ReservationOrderEntity> findByUserIdOrderByReservedAtDesc(Long userId);
}
