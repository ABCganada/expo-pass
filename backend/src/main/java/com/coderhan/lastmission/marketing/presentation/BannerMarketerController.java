package com.coderhan.lastmission.marketing.presentation;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마케터용 광고 등록/수정 API.
 * PENDING 상태 광고만 수정 가능하며, 본인이 등록한 광고만 수정할 수 있다.
 */
@RestController
@RequestMapping("/api/v1/marketer/banner-ads")
@RequiredArgsConstructor
class BannerMarketerController {
    private final BannerAdService bannerAdService;

    @PostMapping
    ResponseEntity<ApiResponse<BannerAdResponse>> registerAd(
            @RequestBody RegisterAdRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        BannerAd ad = bannerAdService.registerAd(
                request.slotId(), request.title(), request.imageUrl(), request.linkUrl(),
                request.priority(), request.startsAt(), request.endsAt(), principal.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(BannerAdResponse.from(ad)));
    }

    @PutMapping("/{id}")
    ApiResponse<BannerAdResponse> updateAd(
            @PathVariable UUID id,
            @RequestBody UpdateAdRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        BannerAd ad = bannerAdService.updateAd(
                id, principal.email(), request.title(), request.imageUrl(), request.linkUrl(),
                request.priority(), request.startsAt(), request.endsAt());
        return ApiResponse.success(BannerAdResponse.from(ad));
    }

    record RegisterAdRequest(
            UUID slotId,
            String title,
            String imageUrl,
            String linkUrl,
            int priority,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {}

    record UpdateAdRequest(
            String title,
            String imageUrl,
            String linkUrl,
            int priority,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {}

    record BannerAdResponse(
            UUID id,
            UUID slotId,
            String title,
            String imageUrl,
            String linkUrl,
            int priority,
            BannerAdStatus status,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String createdBy,
            OffsetDateTime createdAt
    ) {
        static BannerAdResponse from(BannerAd ad) {
            return new BannerAdResponse(ad.id(), ad.slotId(), ad.title(), ad.imageUrl(), ad.linkUrl(),
                    ad.priority(), ad.status(), ad.startsAt(), ad.endsAt(), ad.createdBy(), ad.createdAt());
        }
    }
}
