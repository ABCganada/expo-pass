package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.reservation.ReservationOrderDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaReservationOrderDirectory implements ReservationOrderDirectory {
    private final ReservationOrderJpaRepository orderJpaRepository;

    @Override
    public Optional<BigDecimal> findOrderAmount(String orderId) {
        return orderJpaRepository.findById(orderId).map(ReservationOrderEntity::getTotalAmount);
    }

    @Override
    public Optional<Long> findEventIdByOrderId(String orderId) {
        return orderJpaRepository.findById(orderId).map(ReservationOrderEntity::getEventId);
    }

    @Override
    public List<String> findOrderIdsByEventId(long eventId) {
        return orderJpaRepository.findOrderIdByEventId(eventId);
    }
}
