package com.coderhan.lastmission.marketing.presentation;

import java.util.HashSet;
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
            Set<String> slotTypes,
            String title,
            String bannerImageUrl,
            String adImageUrl,
            String linkUrl
    ) {
        static BannerAdResponse from(BannerAd ad) {
            Set<String> slotTypes = new HashSet<>();
            if (ad.bannerImageUrl() != null && !ad.bannerImageUrl().isBlank()) slotTypes.add("BANNER");
            if (ad.adImageUrl() != null && !ad.adImageUrl().isBlank()) slotTypes.add("TAB");
            return new BannerAdResponse(ad.id(), ad.slotIds(), slotTypes, ad.title(),
                    ad.bannerImageUrl(), ad.adImageUrl(), ad.linkUrl());
        }
    }
}
