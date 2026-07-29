package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerStatRepository;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaBannerStatRepository implements BannerStatRepository {
    private final BannerImpressionJpaRepository impressionJpa;
    private final BannerClickJpaRepository clickJpa;

    @Override
    public void incrementImpression(UUID adId, LocalDate date) {
        impressionJpa.upsertIncrement(adId, date);
    }

    @Override
    public void incrementClick(UUID adId, LocalDate date) {
        clickJpa.upsertIncrement(adId, date);
    }

    @Override
    public BannerAdStats sumStats(UUID adId) {
        long impressions = impressionJpa.sumCountByAdId(adId);
        long clicks = clickJpa.sumCountByAdId(adId);
        return BannerAdStats.of(adId, impressions, clicks);
    }

    @Override
    public BannerAdStats sumStatsByDateRange(UUID adId, LocalDate from, LocalDate to) {
        long impressions = impressionJpa.sumCountByAdIdAndDateRange(adId, from, to);
        long clicks = clickJpa.sumCountByAdIdAndDateRange(adId, from, to);
        return BannerAdStats.of(adId, impressions, clicks);
    }
}
