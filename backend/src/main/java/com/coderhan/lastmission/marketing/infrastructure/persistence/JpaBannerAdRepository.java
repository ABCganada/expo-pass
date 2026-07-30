package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdRepository;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaBannerAdRepository implements BannerAdRepository {
    private final BannerAdJpaRepository jpaRepository;

    @Override
    public BannerAd save(Set<UUID> slotIds, String title, String bannerImageUrl, String adImageUrl,
                         String linkUrl, int priority, OffsetDateTime startsAt, OffsetDateTime endsAt,
                         String createdBy, long totalAmount) {
        BannerAdEntity entity = new BannerAdEntity(
                null, slotIds, title, bannerImageUrl, adImageUrl, linkUrl, priority,
                BannerAdStatus.PENDING, startsAt, endsAt, createdBy, OffsetDateTime.now(), totalAmount);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<BannerAd> findById(UUID id) {
        return jpaRepository.findById(id).map(JpaBannerAdRepository::toDomain);
    }

    @Override
    public List<BannerAd> findActiveBySlot(UUID slotId, OffsetDateTime now) {
        return jpaRepository.findActiveBySlot(slotId, BannerAdStatus.APPROVED, now).stream()
                .map(JpaBannerAdRepository::toDomain)
                .toList();
    }

    @Override
    public List<BannerAd> findAllActive(OffsetDateTime now) {
        return jpaRepository.findAllActive(BannerAdStatus.APPROVED, now).stream()
                .map(JpaBannerAdRepository::toDomain)
                .toList();
    }

    @Override
    public BannerAd updateStatus(UUID id, BannerAdStatus status) {
        BannerAdEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("BannerAd not found: " + id));
        entity.setStatus(status);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public BannerAd update(UUID id, String title, String bannerImageUrl, String adImageUrl,
                           String linkUrl, int priority, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        BannerAdEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("BannerAd not found: " + id));
        entity.update(title, bannerImageUrl, adImageUrl, linkUrl, priority, startsAt, endsAt);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<BannerAd> findByCreatedBy(String email) {
        return jpaRepository.findByCreatedByOrderByCreatedAtDesc(email).stream()
                .map(JpaBannerAdRepository::toDomain)
                .toList();
    }

    @Override
    public List<BannerAd> findAll() {
        return jpaRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")).stream()
                .map(JpaBannerAdRepository::toDomain)
                .toList();
    }

    @Override
    public List<BannerAd> findExpiredApproved(OffsetDateTime now) {
        return jpaRepository.findExpiredApproved(BannerAdStatus.APPROVED, now).stream()
                .map(JpaBannerAdRepository::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static BannerAd toDomain(BannerAdEntity entity) {
        return new BannerAd(entity.getId(), entity.getSlotIds(), entity.getTitle(),
                entity.getBannerImageUrl(), entity.getAdImageUrl(),
                entity.getLinkUrl(), entity.getPriority(), entity.getStatus(),
                entity.getStartsAt(), entity.getEndsAt(), entity.getCreatedBy(),
                entity.getCreatedAt(), entity.getTotalAmount());
    }
}
