package com.coderhan.lastmission.marketing.presentation;

import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerStatService;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객용 배너 노출/클릭 기록 API.
 * 인증 없이 접근 가능하며, 프론트에서 배너 노출·클릭 시 호출한다.
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
class BannerStatController {
    private final BannerStatService bannerStatService;

    @PostMapping("/{id}/impressions")
    ResponseEntity<ApiResponse<Void>> recordImpression(@PathVariable UUID id) {
        bannerStatService.recordImpression(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PostMapping("/{id}/clicks")
    ResponseEntity<ApiResponse<Void>> recordClick(@PathVariable UUID id) {
        bannerStatService.recordClick(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }
}
