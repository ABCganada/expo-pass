package com.coderhan.lastmission.marketing.presentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.application.BannerSlotService;
import com.coderhan.lastmission.marketing.application.BannerStatService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 광고 슬롯 관리 및 광고 승인/거절 API.
 */
@RestController
@RequestMapping("/api/v1/admin/banner")
@RequiredArgsConstructor
class BannerAdminController {
    private final BannerSlotService bannerSlotService;
    private final BannerAdService bannerAdService;
    private final BannerStatService bannerStatService;

    // --- 슬롯 관리 ---

    @PostMapping("/slots")
    ResponseEntity<ApiResponse<BannerSlotResponse>> createSlot(@RequestBody CreateSlotRequest request) {
        BannerSlot slot = bannerSlotService.createSlot(request.name(), request.maxCount());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(BannerSlotResponse.from(slot)));
    }

    @GetMapping("/slots")
    ApiResponse<List<BannerSlotResponse>> getSlots() {
        List<BannerSlotResponse> responses = bannerSlotService.getSlots().stream()
                .map(BannerSlotResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/slots/{slotId}")
    ApiResponse<BannerSlotResponse> getSlot(@PathVariable UUID slotId) {
        return ApiResponse.success(BannerSlotResponse.from(bannerSlotService.getSlot(slotId)));
    }

    // --- 광고 승인/거절 ---

    @PostMapping("/ads/{id}/approve")
    ApiResponse<BannerAdResponse> approve(@PathVariable UUID id) {
        return ApiResponse.success(BannerAdResponse.from(bannerAdService.approve(id)));
    }

    @PostMapping("/ads/{id}/reject")
    ApiResponse<BannerAdResponse> reject(@PathVariable UUID id) {
        return ApiResponse.success(BannerAdResponse.from(bannerAdService.reject(id)));
    }

    // --- 통계 조회 ---

    @GetMapping("/ads/{id}/stats")
    ApiResponse<BannerStatsResponse> getStats(@PathVariable UUID id) {
        return ApiResponse.success(BannerStatsResponse.from(bannerStatService.getStats(id)));
    }

    record CreateSlotRequest(String name, int maxCount) {}

    record BannerStatsResponse(UUID adId, long impressions, long clicks, double ctr) {
        static BannerStatsResponse from(BannerAdStats stats) {
            return new BannerStatsResponse(stats.adId(), stats.impressions(), stats.clicks(), stats.ctr());
        }
    }

    record BannerSlotResponse(UUID id, String name, int maxCount, OffsetDateTime createdAt) {
        static BannerSlotResponse from(BannerSlot slot) {
            return new BannerSlotResponse(slot.id(), slot.name(), slot.maxCount(), slot.createdAt());
        }
    }

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
