package com.coderhan.lastmission.marketing.domain;

import java.util.UUID;

public record BannerAdStats(UUID adId, long impressions, long clicks, double ctr) {}
