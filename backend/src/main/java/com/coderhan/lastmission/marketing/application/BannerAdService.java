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
    public BannerAd registerAd(Set<UUID> slotIds, String title, String imageUrl, String linkUrl,
                               int priority, OffsetDateTime startsAt, OffsetDateTime endsAt, String createdBy) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 슬롯을 하나 이상 선택해야 합니다.");
        }
        List<BannerSlot> slots = slotRepository.findAllByIds(slotIds);
        if (slots.size() != slotIds.size()) {
            throw new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "존재하지 않는 슬롯이 포함되어 있습니다.");
        }
        BannerAd.validate(title, imageUrl, startsAt, endsAt);
        return adRepository.save(slotIds, title, imageUrl, linkUrl, priority, startsAt, endsAt, createdBy);
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

    /**
     * 어드민이 광고를 수락한다. PENDING → CONFIRMED.
     * 슬롯 단가 × 기간(일)으로 totalAmount를 계산하고 저장한다.
     * 이후 광고주가 결제를 완료하면 approveAfterPayment()를 통해 APPROVED로 전환된다.
     */
    @Transactional
    public BannerAd confirm(UUID id) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "이미 처리된 광고입니다.");
        }
        long totalAmount = calculateTotalAmount(ad);
        return adRepository.confirm(id, totalAmount);
    }

    /**
     * 결제 완료 후 광고를 APPROVED로 전환한다.
     * payment 도메인 담당자가 결제 성공 콜백 시 이 메서드를 호출해야 한다.
     */
    @Transactional
    public BannerAd approveAfterPayment(UUID id) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.status() != BannerAdStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "결제 대기(CONFIRMED) 상태의 광고만 승인할 수 있습니다.");
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

    private long calculateTotalAmount(BannerAd ad) {
        long days = Math.max(1, ChronoUnit.DAYS.between(
                ad.startsAt().toLocalDate(), ad.endsAt().toLocalDate()));
        long dailyTotal = slotRepository.findAllByIds(ad.slotIds()).stream()
                .mapToLong(BannerSlot::pricePerDay)
                .sum();
        return dailyTotal * days;
    }
}
