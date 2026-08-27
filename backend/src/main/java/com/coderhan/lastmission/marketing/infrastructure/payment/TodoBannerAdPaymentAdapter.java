package com.coderhan.lastmission.marketing.infrastructure.payment;

import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerAdPaymentPort;
import com.coderhan.lastmission.marketing.application.BannerAdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link BannerAdPaymentPort} 임시 구현체.
 *
 * TODO: payment 도메인 담당자와 협의 후 실제 Toss 결제 연동으로 교체.
 *   - payment 도메인에서 이 어댑터의 getTotalAmount()를 호출해 금액 검증
 *   - 결제 성공 후 BannerAdService.approveAfterPayment() 호출 필요
 */
@Component
@RequiredArgsConstructor
public class TodoBannerAdPaymentAdapter implements BannerAdPaymentPort {

    private final BannerAdService bannerAdService;

    @Override
    public long getTotalAmount(UUID bannerAdId) {
        return bannerAdService.getAd(bannerAdId).totalAmount();
    }
}
