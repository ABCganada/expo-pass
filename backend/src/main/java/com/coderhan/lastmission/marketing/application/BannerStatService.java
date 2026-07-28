package com.coderhan.lastmission.marketing.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerStatService {
    private final BannerStatRepository statRepository;
    private final BannerAdRepository adRepository;
    private final Clock clock;

    @Transactional
    public void recordImpression(UUID adId) {
        adRepository.findById(adId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + adId));
        statRepository.incrementImpression(adId, LocalDate.now(clock));
    }

    @Transactional
    public void recordClick(UUID adId) {
        adRepository.findById(adId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + adId));
        statRepository.incrementClick(adId, LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public BannerAdStats getStats(UUID adId) {
        adRepository.findById(adId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_AD_NOT_FOUND, "광고를 찾을 수 없습니다. id=" + adId));
        return statRepository.sumStats(adId);
    }
}
