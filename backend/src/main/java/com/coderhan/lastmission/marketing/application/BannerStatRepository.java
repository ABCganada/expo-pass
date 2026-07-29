package com.coderhan.lastmission.marketing.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerDailyStat;

public interface BannerStatRepository {
    void incrementImpression(UUID adId, LocalDate date);
    void incrementClick(UUID adId, LocalDate date);
    BannerAdStats sumStats(UUID adId);
    BannerAdStats sumStatsByDateRange(UUID adId, LocalDate from, LocalDate to);
    List<BannerDailyStat> findDailyStats(UUID adId, LocalDate from, LocalDate to);
}
