package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.util.List;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationOrderJpaRepository extends JpaRepository<ReservationOrderEntity, String> {
    List<ReservationOrderEntity> findByUserIdOrderByReservedAtDesc(Long userId);

    // 관리자 예약자 명단 조회용
    List<ReservationOrderEntity> findByEventIdOrderByReservedAtDesc(Long eventId);

    // 관리자 예약 현황(상태별 건수) 조회용
    @Query("SELECT o.status as status, COUNT(o) as count FROM ReservationOrderEntity o "
            + "WHERE o.eventId = :eventId GROUP BY o.status")
    List<StatusCount> countByEventIdGroupByStatus(@Param("eventId") Long eventId);

    interface StatusCount {
        OrderStatus getStatus();
        long getCount();
    }
}
