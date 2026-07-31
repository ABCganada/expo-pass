package com.coderhan.lastmission.marketing.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;

public interface BannerAdRepository {
    BannerAd save(Set<UUID> slotIds, String orderId, String title, String bannerImageUrl, String adImageUrl,
                  String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt,
                  String createdBy, long totalAmount);
    Optional<BannerAd> findById(UUID id);
    List<BannerAd> findActiveBySlot(UUID slotId, OffsetDateTime now);
    List<BannerAd> findAllActive(OffsetDateTime now);
    BannerAd updateStatus(UUID id, BannerAdStatus status);
    BannerAd update(UUID id, String title, String bannerImageUrl, String adImageUrl,
                    String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt);
    List<BannerAd> findByCreatedBy(String email);
    List<BannerAd> findAll();
    List<BannerAd> findExpiredApproved(OffsetDateTime now);
    void deleteById(UUID id);
    boolean existsActiveOrPendingBySlotId(UUID slotId);
}
