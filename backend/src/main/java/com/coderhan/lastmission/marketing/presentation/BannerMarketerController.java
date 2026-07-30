package com.coderhan.lastmission.marketing.presentation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.application.BannerStatService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마케터용 광고 등록/수정 API.
 * PENDING 상태 광고만 수정 가능하며, 본인이 등록한 광고만 수정할 수 있다.
 */
@RestController
@RequestMapping("/api/v1/manager/banner-ads")
@RequiredArgsConstructor
class BannerMarketerController {
    private final BannerAdService bannerAdService;
    private final BannerStatService bannerStatService;

    @GetMapping
    ApiResponse<List<BannerAdResponse>> getMyAds(@AuthenticationPrincipal LastMissionPrincipal principal) {
        List<BannerAdResponse> responses = bannerAdService.getMyAds(principal.email()).stream()
                .map(BannerAdResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    ResponseEntity<ApiResponse<BannerAdResponse>> registerAd(
            @RequestBody RegisterAdRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        BannerAd ad = bannerAdService.registerAd(
                request.slotIds(), request.title(), request.imageUrl(), request.linkUrl(),
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

    @GetMapping("/{id}/stats")
    ApiResponse<BannerStatsResponse> getStats(
            @PathVariable UUID id,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        BannerAdStats stats = bannerStatService.getStatsByDateRange(id, from, to);
        return ApiResponse.success(BannerStatsResponse.from(stats));
    }

    @GetMapping("/{id}/stats/export")
    ResponseEntity<byte[]> exportStats(
            @PathVariable UUID id,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        byte[] xlsx = bannerStatService.exportStatsByDateRange(id, from, to);
        String filename = "banner-stats-" + id + "-" + from + "-" + to + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(xlsx);
    }

    record RegisterAdRequest(
            Set<UUID> slotIds,
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

    record BannerStatsResponse(UUID adId, long impressions, long clicks, double ctr) {
        static BannerStatsResponse from(BannerAdStats stats) {
            return new BannerStatsResponse(stats.adId(), stats.impressions(), stats.clicks(), stats.ctr());
        }
    }

    record BannerAdResponse(
            UUID id,
            Set<UUID> slotIds,
            String title,
            String imageUrl,
            String linkUrl,
            int priority,
            BannerAdStatus status,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String createdBy,
            OffsetDateTime createdAt,
            Long totalAmount
    ) {
        static BannerAdResponse from(BannerAd ad) {
            return new BannerAdResponse(ad.id(), ad.slotIds(), ad.title(), ad.imageUrl(), ad.linkUrl(),
                    ad.priority(), ad.status(), ad.startsAt(), ad.endsAt(), ad.createdBy(),
                    ad.createdAt(), ad.totalAmount());
        }
    }
}
