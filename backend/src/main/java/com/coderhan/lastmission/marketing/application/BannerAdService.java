package com.coderhan.lastmission.marketing.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerAdService {
    private final BannerAdRepository adRepository;
    private final BannerSlotRepository slotRepository;
    private final BannerStatRepository statRepository;
    private final Clock clock;

    @Transactional
    public BannerAd registerAd(Set<UUID> slotIds, String title, String bannerImageUrl, String adImageUrl,
                               String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               String createdBy) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 슬롯을 하나 이상 선택해야 합니다.");
        }
        List<BannerSlot> slots = slotRepository.findAllByIds(slotIds);
        if (slots.size() != slotIds.size()) {
            throw new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "존재하지 않는 슬롯이 포함되어 있습니다.");
        }
        BannerAd.validate(title, startsAt, endsAt);
        validateImages(slots, bannerImageUrl, adImageUrl);

        int days = (int) Math.max(1, ChronoUnit.DAYS.between(startsAt.toLocalDate(), endsAt.toLocalDate()));
        long totalAmount = calculateTotalAmount(slots, days);

        return adRepository.save(slotIds, title, bannerImageUrl, adImageUrl, linkUrl,
                startsAt, endsAt, createdBy, totalAmount);
    }

    @Transactional
    public BannerAd updateAd(UUID id, String createdBy, String title, String bannerImageUrl, String adImageUrl,
                              String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (!ad.createdBy().equals(createdBy)) {
            throw new BusinessException(ErrorCode.BANNER_AD_ACCESS_DENIED, "본인의 광고만 수정할 수 있습니다.");
        }
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "승인/거절된 광고는 수정할 수 없습니다.");
        }
        BannerAd.validate(title, startsAt, endsAt);
        return adRepository.update(id, title, bannerImageUrl, adImageUrl, linkUrl, startsAt, endsAt);
    }

    @Transactional
    public BannerAd approve(UUID id) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "이미 처리된 광고입니다.");
        }
        return adRepository.updateStatus(id, BannerAdStatus.APPROVED);
    }

    @Transactional
    public BannerAd reject(UUID id) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "이미 처리된 광고입니다.");
        }
        return adRepository.updateStatus(id, BannerAdStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getActiveBanners() {
        return adRepository.findAllActive(OffsetDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getMyAds(String email) {
        return adRepository.findByCreatedBy(email);
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getAllAds() {
        return adRepository.findAll();
    }

    @Transactional
    public void expireAds() {
        adRepository.findExpiredApproved(OffsetDateTime.now(clock))
                .forEach(ad -> adRepository.updateStatus(ad.id(), BannerAdStatus.EXPIRED));
    }

    @Transactional
    public void deleteAd(UUID id) {
        adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        statRepository.deleteStatsByAdId(id);
        adRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public BannerAd getAd(UUID id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
    }

    private void validateImages(List<BannerSlot> slots, String bannerImageUrl, String adImageUrl) {
        boolean needsBanner = slots.stream().anyMatch(s -> s.type() == BannerSlotType.BANNER);
        boolean needsAd = slots.stream().anyMatch(s -> s.type() == BannerSlotType.TAB);
        if (needsBanner && (bannerImageUrl == null || bannerImageUrl.isBlank())) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "배너 슬롯은 배너 이미지가 필요합니다.");
        }
        if (needsAd && (adImageUrl == null || adImageUrl.isBlank())) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 탭 슬롯은 광고 이미지가 필요합니다.");
        }
    }

    private long calculateTotalAmount(List<BannerSlot> slots, int days) {
        return slots.stream()
                .mapToLong(slot -> slot.pricePerDay() * days)
                .sum();
    }
}
