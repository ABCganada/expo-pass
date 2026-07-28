package com.coderhan.lastmission.marketing.domain;

import java.time.LocalDate;
import java.util.UUID;

public record BannerImpression(UUID id, UUID adId, LocalDate statDate, long count) {}
