package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BannerImpressionJpaRepository extends JpaRepository<BannerImpressionEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO marketing_banner_impressions (id, ad_id, stat_date, count)
            VALUES (gen_random_uuid(), :adId, :date, 1)
            ON CONFLICT (ad_id, stat_date) DO UPDATE SET count = marketing_banner_impressions.count + 1
            """, nativeQuery = true)
    void upsertIncrement(@Param("adId") UUID adId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.count), 0) FROM BannerImpressionEntity e WHERE e.adId = :adId")
    long sumCountByAdId(@Param("adId") UUID adId);

    @Query("SELECT COALESCE(SUM(e.count), 0) FROM BannerImpressionEntity e WHERE e.adId = :adId AND e.statDate BETWEEN :from AND :to")
    long sumCountByAdIdAndDateRange(@Param("adId") UUID adId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
