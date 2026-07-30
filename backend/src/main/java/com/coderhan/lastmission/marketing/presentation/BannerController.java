package com.coderhan.lastmission.marketing.presentation;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객용 배너 조회 API.
 * 인증 없이 접근 가능하며, 현재 활성 상태인 광고(APPROVED + 기간 내)를 반환한다.
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
class BannerController {
    private final BannerAdService bannerAdService;

    @GetMapping
    ApiResponse<List<BannerAdResponse>> getActiveBanners() {
        List<BannerAdResponse> responses = bannerAdService.getActiveBanners().stream()
                .map(BannerAdResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    record BannerAdResponse(
            UUID id,
            Set<UUID> slotIds,
            String title,
            String bannerImageUrl,
            String adImageUrl,
            String linkUrl,
            int priority
    ) {
        static BannerAdResponse from(BannerAd ad) {
            return new BannerAdResponse(ad.id(), ad.slotIds(), ad.title(),
                    ad.bannerImageUrl(), ad.adImageUrl(), ad.linkUrl(), ad.priority());
        }
    }
}
