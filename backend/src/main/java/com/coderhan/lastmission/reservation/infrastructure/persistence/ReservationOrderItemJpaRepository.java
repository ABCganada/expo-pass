package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationOrderItemJpaRepository extends JpaRepository<ReservationOrderItemEntity, Long> {
    List<ReservationOrderItemEntity> findByOrderId(String orderId);
    Optional<ReservationOrderItemEntity> findByQrCodeHash(String qrCodeHash);

    // checked_in_at이 아직 NULL일 때만 성공 — 같은 QR이 동시에 두 번 스캔돼도 하나만 처리됨
    @Modifying
    @Query("""
            UPDATE ReservationOrderItemEntity i
            SET i.checkedInAt = :now, i.checkedInByAdminId = :adminUserId
            WHERE i.qrCodeHash = :qrCodeHash AND i.checkedInAt IS NULL
            """)
    int checkin(@Param("qrCodeHash") String qrCodeHash, @Param("adminUserId") long adminUserId,
                @Param("now") OffsetDateTime now);
}
