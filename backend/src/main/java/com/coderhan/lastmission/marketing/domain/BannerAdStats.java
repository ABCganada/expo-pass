package com.coderhan.lastmission.marketing.domain;

import java.util.UUID;

public record BannerAdStats(UUID adId, long impressions, long clicks, double ctr) {
    public static BannerAdStats of(UUID adId, long impressions, long clicks) {
        double ctr = impressions == 0 ? 0.0 : (double) clicks / impressions * 100;
        return new BannerAdStats(adId, impressions, clicks, ctr);
    }
}
