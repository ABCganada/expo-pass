package com.coderhan.lastmission.marketing.application;

import java.time.LocalDate;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;

public interface BannerStatRepository {
    void incrementImpression(UUID adId, LocalDate date);
    void incrementClick(UUID adId, LocalDate date);
    BannerAdStats sumStats(UUID adId);
}
