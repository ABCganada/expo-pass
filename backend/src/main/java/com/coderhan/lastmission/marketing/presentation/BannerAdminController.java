package com.coderhan.lastmission.marketing.presentation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import com.coderhan.lastmission.marketing.application.BannerPricingPolicyService;
import com.coderhan.lastmission.marketing.application.BannerSlotService;
import com.coderhan.lastmission.marketing.application.BannerStatService;
import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerPricingPolicy;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 광고 슬롯 관리 및 광고 승인(APPROVED)/거절 API.
 * 광고주가 결제를 완료하면 PENDING 상태가 되고, 관리자가 승인하면 APPROVED로 전환되어 광고가 노출된다.
 */
@RestController
@RequestMapping("/api/v1/admin/banner")
@RequiredArgsConstructor
class BannerAdminController {
    private final BannerSlotService bannerSlotService;
    private final BannerAdService bannerAdService;
    private final BannerStatService bannerStatService;
    private final BannerPricingPolicyService bannerPricingPolicyService;

    // --- 슬롯 관리 ---

    @PostMapping("/slots")
    ResponseEntity<ApiResponse<BannerSlotResponse>> createSlot(@RequestBody CreateSlotRequest request) {
        BannerSlot slot = bannerSlotService.createSlot(request.name(), request.maxCount(), request.type());
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

    // --- 가격 정책 관리 ---

    @PostMapping("/slots/{slotId}/policies")
    ResponseEntity<ApiResponse<BannerPricingPolicyResponse>> createPolicy(
            @PathVariable UUID slotId,
            @RequestBody CreatePolicyRequest request) {
        BannerPricingPolicy policy = bannerPricingPolicyService.createPolicy(slotId, request.durationDays(), request.price());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(BannerPricingPolicyResponse.from(policy)));
    }

    @GetMapping("/slots/{slotId}/policies")
    ApiResponse<List<BannerPricingPolicyResponse>> getPolicies(@PathVariable UUID slotId) {
        List<BannerPricingPolicyResponse> responses = bannerPricingPolicyService.getPoliciesBySlot(slotId).stream()
                .map(BannerPricingPolicyResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/slots/{slotId}/policies/{policyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePolicy(@PathVariable UUID slotId, @PathVariable UUID policyId) {
        bannerPricingPolicyService.deletePolicy(policyId);
    }

    // --- 광고 목록 (전체) ---

    @GetMapping("/ads")
    ApiResponse<List<BannerAdResponse>> getAllAds() {
        List<BannerAdResponse> responses = bannerAdService.getAllAds().stream()
                .map(BannerAdResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    // --- 광고 수락/거절 ---

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

    // --- 통계 조회 ---

    @GetMapping("/ads/{id}/stats")
    ApiResponse<BannerStatsResponse> getStats(@PathVariable UUID id) {
        return ApiResponse.success(BannerStatsResponse.from(bannerStatService.getStats(id)));
    }

    record CreateSlotRequest(String name, int maxCount, BannerSlotType type) {}

    record CreatePolicyRequest(int durationDays, long price) {}

    record BannerStatsResponse(UUID adId, long impressions, long clicks, double ctr) {
        static BannerStatsResponse from(BannerAdStats stats) {
            return new BannerStatsResponse(stats.adId(), stats.impressions(), stats.clicks(), stats.ctr());
        }
    }

    record BannerSlotResponse(UUID id, String name, int maxCount, BannerSlotType type, OffsetDateTime createdAt) {
        static BannerSlotResponse from(BannerSlot slot) {
            return new BannerSlotResponse(slot.id(), slot.name(), slot.maxCount(), slot.type(), slot.createdAt());
        }
    }

    record BannerPricingPolicyResponse(UUID id, UUID slotId, int durationDays, long price, OffsetDateTime createdAt) {
        static BannerPricingPolicyResponse from(BannerPricingPolicy policy) {
            return new BannerPricingPolicyResponse(policy.id(), policy.slotId(), policy.durationDays(), policy.price(), policy.createdAt());
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
