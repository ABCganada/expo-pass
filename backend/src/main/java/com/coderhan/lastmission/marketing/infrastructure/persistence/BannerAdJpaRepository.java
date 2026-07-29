package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BannerAdJpaRepository extends JpaRepository<BannerAdEntity, UUID> {

    @Query("SELECT a FROM BannerAdEntity a WHERE a.slotId = :slotId AND a.status = :status AND a.startsAt <= :now AND a.endsAt >= :now ORDER BY a.priority ASC")
    List<BannerAdEntity> findActiveBySlot(@Param("slotId") UUID slotId,
                                          @Param("status") BannerAdStatus status,
                                          @Param("now") OffsetDateTime now);

    @Query("SELECT a FROM BannerAdEntity a WHERE a.status = :status AND a.startsAt <= :now AND a.endsAt >= :now ORDER BY a.priority ASC")
    List<BannerAdEntity> findAllActive(@Param("status") BannerAdStatus status,
                                       @Param("now") OffsetDateTime now);

    List<BannerAdEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
