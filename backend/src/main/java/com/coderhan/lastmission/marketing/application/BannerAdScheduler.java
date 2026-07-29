package com.coderhan.lastmission.marketing.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BannerAdScheduler {
    private final BannerAdService bannerAdService;

    /**
     * 매 분 정각에 종료 기간이 지난 APPROVED 광고를 EXPIRED 로 전환한다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void expireAds() {
        bannerAdService.expireAds();
    }
}
