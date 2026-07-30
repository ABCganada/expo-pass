package com.coderhan.lastmission.payment.infrastructure.toss;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import com.coderhan.lastmission.payment.application.PaymentGateway;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 결제 승인(confirm) API 연동
 */
@Component
class TossPaymentGateway implements PaymentGateway {

    private final RestClient restClient;

    TossPaymentGateway(
        @Value("${lastmission.toss.base-url}") String baseUrl,
        @Value("${lastmission.toss.secret-key}") String secretKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
                .build();
    }

    @Override
    public ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount) {
        long krwAmount = toKrwAmount(amount);

        try {
            TossConfirmResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new TossConfirmRequest(paymentKey, pgOrderId, krwAmount))
                    .retrieve()
                    .body(TossConfirmResponse.class);

            return new ConfirmResult(response.method(), response.approvedAt());
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
                    "토스 결제 승인에 실패했습니다: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public CancelResult cancel(String paymentKey, String reason, BigDecimal amount) {
        try {
            TossCancelResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new TossCancelRequest(reason, toKrwAmount(amount)))
                    .retrieve()
                    .body(TossCancelResponse.class);

            return new CancelResult(response.latestCancelledAt());
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_CANCEL_FAILED,
                    "토스 결제취소에 실패했습니다: " + e.getResponseBodyAsString());
        }
    }

    private static long toKrwAmount(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "결제 금액이 올바르지 않습니다.");
        }
    }

    private static String basicAuth(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private record TossConfirmRequest(String paymentKey, String orderId, long amount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossConfirmResponse(String method, OffsetDateTime approvedAt) {}

    private record TossCancelRequest(String cancelReason, long cancelAmount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossCancelResponse(List<CancelDetail> cancels) {
        /** 취소 이력 중 가장 최근 건 — 환불 1건당 취소 호출은 정확히 1번만 일어나므로 항상 마지막 항목이 이 호출의 결과다. */
        OffsetDateTime latestCancelledAt() {
            return cancels.get(cancels.size() - 1).cancelledAt();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record CancelDetail(OffsetDateTime cancelledAt) {}
    }
}
