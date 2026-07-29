package com.coderhan.lastmission.payment.infrastructure.toss;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
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
}
