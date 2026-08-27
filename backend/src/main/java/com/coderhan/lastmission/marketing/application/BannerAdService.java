package com.coderhan.lastmission.marketing.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.AdExpiredEvent;
import com.coderhan.lastmission.marketing.AdRejectedEvent;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerAdService {
    private final BannerAdRepository adRepository;
    private final BannerSlotRepository slotRepository;
    private final BannerStatRepository statRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public BannerAd registerAd(Set<UUID> slotIds, String title, String bannerImageUrl, String adImageUrl,
                               String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt,
                               long createdBy) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "광고 슬롯을 하나 이상 선택해야 합니다.");
        }
        List<BannerSlot> slots = slotRepository.findAllByIds(slotIds);
        if (slots.size() != slotIds.size()) {
            throw new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "존재하지 않는 슬롯이 포함되어 있습니다.");
        }
        BannerAd.validate(title, startsAt, endsAt);
        validateImages(slots, bannerImageUrl, adImageUrl);
        validateSlotCapacity(slots, startsAt, endsAt);

        int days = (int) Math.max(1, ChronoUnit.DAYS.between(startsAt.toLocalDate(), endsAt.toLocalDate()));
        long totalAmount = calculateTotalAmount(slots, days);
        String orderId = UUID.randomUUID().toString();

        return adRepository.save(slotIds, orderId, title, bannerImageUrl, adImageUrl, linkUrl,
                startsAt, endsAt, createdBy, totalAmount);
    }

    @Transactional
    public BannerAd updateAd(UUID id, long createdBy, String title, String bannerImageUrl, String adImageUrl,
                              String linkUrl, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.createdBy() != createdBy) {
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
        if (ad.status() == BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_PAYMENT_REQUIRED, "결제가 완료되지 않은 광고입니다.");
        }
        if (ad.status() != BannerAdStatus.PAID) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "이미 처리된 광고입니다.");
        }
        return adRepository.updateStatus(id, BannerAdStatus.APPROVED);
    }

    /**
     * 광고 반려. 결제완료(PAID) 상태만 반려 가능하며, 반려되면 {@link AdRejectedEvent}를 발행해
     * payment 모듈이 결제를 자동 환불하도록 한다(광고 환불 정책 — 항상 100%).
     */
    @Transactional
    public BannerAd reject(UUID id) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.status() == BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_PAYMENT_REQUIRED, "결제가 완료되지 않은 광고입니다.");
        }
        if (ad.status() != BannerAdStatus.PAID) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "이미 처리된 광고입니다.");
        }
        BannerAd rejected = adRepository.updateStatus(id, BannerAdStatus.REJECTED);
        eventPublisher.publishEvent(new AdRejectedEvent(rejected.id(), rejected.orderId()));
        return rejected;
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getActiveBanners() {
        return adRepository.findAllActive(OffsetDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getMyAds(long userId) {
        return adRepository.findByCreatedBy(userId);
    }

    @Transactional(readOnly = true)
    public List<BannerAd> getAllAds() {
        return adRepository.findAll();
    }

    @Transactional
    public void expireAds() {
        adRepository.findExpiredApproved(OffsetDateTime.now(clock))
                .forEach(ad -> {
                    adRepository.updateStatus(ad.id(), BannerAdStatus.EXPIRED);
                    if (ad.totalAmount() != null) {
                        eventPublisher.publishEvent(new AdExpiredEvent(ad.id(), ad.totalAmount()));
                    }
                });
    }

    @Transactional
    public void deleteAdByMarketer(UUID id, long requesterId) {
        BannerAd ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        if (ad.createdBy() != requesterId) {
            throw new BusinessException(ErrorCode.BANNER_AD_ACCESS_DENIED, "본인의 광고만 삭제할 수 있습니다.");
        }
        if (ad.status() != BannerAdStatus.PENDING) {
            throw new BusinessException(ErrorCode.BANNER_AD_ALREADY_REVIEWED, "결제 대기 상태의 광고만 삭제할 수 있습니다.");
        }
        statRepository.deleteStatsByAdId(id);
        adRepository.deleteById(id);
    }

    @Transactional
    public void deleteAd(UUID id) {
        adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
        statRepository.deleteStatsByAdId(id);
        adRepository.deleteById(id);
    }

    /** 결제 완료 이벤트 리스너용 — 멱등 (이미 PAID 이상이면 스킵). */
    @Transactional
    public void markAsPaidByOrderId(String orderId) {
        adRepository.findByOrderId(orderId).ifPresent(ad -> {
            if (ad.status() != BannerAdStatus.PENDING) {
                log.warn("markAsPaidByOrderId: 예상 밖 상태. orderId={}, status={}", orderId, ad.status());
                return;
            }
            adRepository.updateStatus(ad.id(), BannerAdStatus.PAID);
        });
    }

    /** 정합성 스케줄러용 — 미결제 타임아웃된 PENDING 광고 취소. */
    @Transactional
    public void cancelByOrderId(String orderId) {
        adRepository.findByOrderId(orderId).ifPresent(ad -> {
            if (ad.status() != BannerAdStatus.PENDING) {
                return;
            }
            adRepository.updateStatus(ad.id(), BannerAdStatus.CANCELLED);
        });
    }

    @Transactional(readOnly = true)
    public BannerAd getAd(UUID id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + id));
    }

    private void validateSlotCapacity(List<BannerSlot> slots, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        List<BannerAdStatus> activeStatuses = List.of(BannerAdStatus.PAID, BannerAdStatus.APPROVED);
        for (BannerSlot slot : slots) {
            int current = adRepository.countOverlappingBySlot(slot.id(), activeStatuses, startsAt, endsAt);
            if (current >= slot.maxCount()) {
                throw new BusinessException(ErrorCode.BANNER_SLOT_CAPACITY_EXCEEDED,
                        "슬롯의 최대 광고 수에 도달했습니다. slot=" + slot.name() + ", max=" + slot.maxCount());
            }
        }
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
