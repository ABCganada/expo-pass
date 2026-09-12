package com.coderhan.lastmission.marketing.application;

import java.util.UUID;

/**
 * 배너 광고 결제 연동 포트.
 *
 * <p>payment 도메인 담당자에게 아래 두 가지 구현 요청 필요:
 * <ol>
 *   <li>결제 금액 검증을 위해 {@link #getTotalAmount(UUID)}를 payment 도메인에서 호출</li>
 *   <li>Toss 결제 승인 완료 후 {@link BannerAdService#approve(UUID)} 호출 (PENDING → APPROVED)</li>
 * </ol>
 *
 * TODO: payment 도메인 담당자와 인터페이스 협의 후 infrastructure 구현체로 교체.
 */
public interface BannerAdPaymentPort {

    /**
     * payment 도메인이 결제 금액 검증 시 호출한다.
     * 광고 등록 시점에 확정된 결제 금액(totalAmount)을 반환한다.
     */
    long getTotalAmount(UUID bannerAdId);
}
