package com.coderhan.lastmission.marketing.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
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
    private final Clock clock;

    @Transactional
    public BannerAd registerAd(UUID slotId, String title, String imageUrl, String linkUrl,
                               int priority, OffsetDateTime startsAt, OffsetDateTime endsAt, String createdBy) {
        slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "광고 슬롯을 찾을 수 없습니다. id=" + slotId));
        BannerAd.validate(title, imageUrl, startsAt, endsAt);
        return adRepository.save(slotId, title, imageUrl, linkUrl, priority, startsAt, endsAt, createdBy);
    }

    @Transactional
    public BannerAd updateAd(UUID id, String createdBy, String title, String imageUrl, String linkUrl,
                              int priority, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (!ad.createdBy().equals(createdBy)) {
            throw new BusinessException(ErrorCode.BANNER_AD_ACCESS_DENIED, "본인의 광고만 수정할 수 있습니다.");
        }
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "승인/거절된 광고는 수정할 수 없습니다.");
        }
        BannerAd.validate(title, imageUrl, startsAt, endsAt);
        return adRepository.update(id, title, imageUrl, linkUrl, priority, startsAt, endsAt);
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
}
