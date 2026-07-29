package com.coderhan.lastmission.marketing.domain;

import java.time.LocalDate;

public record BannerDailyStat(LocalDate date, long impressions, long clicks, double ctr) {

    public static BannerDailyStat of(LocalDate date, long impressions, long clicks) {
        double ctr = impressions == 0 ? 0.0 : (double) clicks / impressions * 100;
        return new BannerDailyStat(date, impressions, clicks, ctr);
    }
}
