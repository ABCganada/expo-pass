package com.coderhan.lastmission.marketing.presentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.application.BannerSlotService;
import com.coderhan.lastmission.marketing.application.BannerStatService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 광고 슬롯 조회 및 광고 승인/거절 API.
 * 슬롯(BANNER/TAB)은 고정이므로 생성·수정·삭제 API는 제공하지 않는다.
 */
@RestController
@RequestMapping("/api/v1/admin/banner")
@RequiredArgsConstructor
class BannerAdminController {
    private final BannerSlotService bannerSlotService;
    private final BannerAdService bannerAdService;
    private final BannerStatService bannerStatService;

    // --- 슬롯 조회 ---

    @GetMapping("/slots")
    ApiResponse<List<BannerSlotResponse>> getSlots() {
        return ApiResponse.success(bannerSlotService.getSlots().stream()
                .map(BannerSlotResponse::from)
                .toList());
    }

    @GetMapping("/slots/{slotId}")
    ApiResponse<BannerSlotResponse> getSlot(@PathVariable UUID slotId) {
        return ApiResponse.success(BannerSlotResponse.from(bannerSlotService.getSlot(slotId)));
    }

    // --- 광고 목록 ---

    @GetMapping("/ads")
    ApiResponse<List<BannerAdResponse>> getAllAds() {
        return ApiResponse.success(bannerAdService.getAllAds().stream()
                .map(BannerAdResponse::from)
                .toList());
    }

    // --- 광고 승인/거절/삭제 ---

    @PostMapping("/ads/{id}/approve")
    ApiResponse<BannerAdResponse> approve(@PathVariable UUID id) {
        return ApiResponse.success(BannerAdResponse.from(bannerAdService.approve(id)));
    }

    @PostMapping("/ads/{id}/reject")
    ApiResponse<BannerAdResponse> reject(@PathVariable UUID id) {
        return ApiResponse.success(BannerAdResponse.from(bannerAdService.reject(id)));
    }

    @DeleteMapping("/ads/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAd(@PathVariable UUID id) {
        bannerAdService.deleteAd(id);
    }

    // --- 통계 ---

    @GetMapping("/ads/{id}/stats")
    ApiResponse<BannerStatsResponse> getStats(@PathVariable UUID id) {
        return ApiResponse.success(BannerStatsResponse.from(bannerStatService.getStats(id)));
    }

    record BannerStatsResponse(UUID adId, long impressions, long clicks, double ctr) {
        static BannerStatsResponse from(BannerAdStats stats) {
            return new BannerStatsResponse(stats.adId(), stats.impressions(), stats.clicks(), stats.ctr());
        }
    }

    record BannerSlotResponse(UUID id, String name, BannerSlotType type, int maxCount, long pricePerDay, OffsetDateTime createdAt) {
        static BannerSlotResponse from(BannerSlot slot) {
            return new BannerSlotResponse(slot.id(), slot.name(), slot.type(), slot.maxCount(), slot.pricePerDay(), slot.createdAt());
        }
    }

    record BannerAdResponse(
            UUID id,
            Set<UUID> slotIds,
            String title,
            String bannerImageUrl,
            String adImageUrl,
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
            return new BannerAdResponse(ad.id(), ad.slotIds(), ad.title(),
                    ad.bannerImageUrl(), ad.adImageUrl(), ad.linkUrl(),
                    ad.priority(), ad.status(), ad.startsAt(), ad.endsAt(), ad.createdBy(),
                    ad.createdAt(), ad.totalAmount());
        }
    }
}
