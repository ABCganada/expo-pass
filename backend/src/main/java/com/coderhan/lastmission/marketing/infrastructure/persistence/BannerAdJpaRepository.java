package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BannerAdJpaRepository extends JpaRepository<BannerAdEntity, UUID> {

    @Query("SELECT DISTINCT a FROM BannerAdEntity a WHERE :slotId MEMBER OF a.slotIds AND a.status = :status AND a.startsAt <= :now AND a.endsAt >= :now ORDER BY a.createdAt ASC")
    List<BannerAdEntity> findActiveBySlot(@Param("slotId") UUID slotId,
                                          @Param("status") BannerAdStatus status,
                                          @Param("now") OffsetDateTime now);

    @Query("SELECT DISTINCT a FROM BannerAdEntity a WHERE a.status = :status AND a.startsAt <= :now AND a.endsAt >= :now ORDER BY a.createdAt ASC")
    List<BannerAdEntity> findAllActive(@Param("status") BannerAdStatus status,
                                       @Param("now") OffsetDateTime now);

    List<BannerAdEntity> findByCreatedByOrderByCreatedAtDesc(long createdBy);

    @Query("SELECT a FROM BannerAdEntity a WHERE a.status = :status AND a.endsAt < :now")
    List<BannerAdEntity> findExpiredApproved(@Param("status") BannerAdStatus status,
                                              @Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(a) > 0 FROM BannerAdEntity a WHERE :slotId MEMBER OF a.slotIds AND a.status IN :statuses")
    boolean existsBySlotIdAndStatusIn(@Param("slotId") UUID slotId,
                                      @Param("statuses") List<BannerAdStatus> statuses);

    java.util.Optional<BannerAdEntity> findByOrderId(String orderId);

    @Query("SELECT a.orderId FROM BannerAdEntity a WHERE a.status = :status AND a.createdAt < :threshold")
    List<String> findOrderIdsByStatusAndCreatedAtBefore(
            @Param("status") BannerAdStatus status,
            @Param("threshold") OffsetDateTime threshold);
}
