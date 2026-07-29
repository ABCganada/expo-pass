package com.coderhan.lastmission.marketing.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;

public interface BannerAdRepository {
    BannerAd save(UUID slotId, String title, String imageUrl, String linkUrl,
                  int priority, OffsetDateTime startsAt, OffsetDateTime endsAt, String createdBy);
    Optional<BannerAd> findById(UUID id);
    List<BannerAd> findActiveBySlot(UUID slotId, OffsetDateTime now);
    List<BannerAd> findAllActive(OffsetDateTime now);
    BannerAd updateStatus(UUID id, BannerAdStatus status);
    BannerAd update(UUID id, String title, String imageUrl, String linkUrl,
                    int priority, OffsetDateTime startsAt, OffsetDateTime endsAt);
    List<BannerAd> findByCreatedBy(String email);
}
