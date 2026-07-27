package com.coderhan.lastmission.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationOrderJpaRepository extends JpaRepository<ReservationOrderEntity, String> {
}
