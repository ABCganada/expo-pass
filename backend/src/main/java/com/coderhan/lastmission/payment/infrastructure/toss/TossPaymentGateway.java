package com.coderhan.lastmission.payment.infrastructure.toss;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import com.coderhan.lastmission.payment.application.PaymentGateway;
import com.coderhan.lastmission.payment.application.PaymentLogRepository;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 결제 승인(confirm)/취소(cancel) API 연동.
 *
 * 호출마다 원본 요청/응답 payload를 payment_logs에 감사 로그로 남긴다(성공/실패 모두).
 * 로그 저장 자체가 실패해도 결제/환불 흐름에는 영향을 주지 않는다(로그는 부가 정보일 뿐,
 * 핵심 흐름을 막을 이유가 없다).
 */
@Slf4j
@Component
class TossPaymentGateway implements PaymentGateway {

    private final RestClient restClient;
    private final PaymentLogRepository paymentLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    TossPaymentGateway(
        @Value("${lastmission.toss.base-url}") String baseUrl,
        @Value("${lastmission.toss.secret-key}") String secretKey,
        PaymentLogRepository paymentLogRepository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
                .build();
        this.paymentLogRepository = paymentLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount) {
        long krwAmount = toKrwAmount(amount);
        String requestPayload = objectMapper.writeValueAsString(new TossConfirmRequest(paymentKey, pgOrderId, krwAmount));

        try {
            String responsePayload = restClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new TossConfirmRequest(paymentKey, pgOrderId, krwAmount))
                    .retrieve()
                    .body(String.class);

            logOutbound(paymentKey, "APPROVE", requestPayload, responsePayload);
            TossConfirmResponse response = objectMapper.readValue(responsePayload, TossConfirmResponse.class);

            return new ConfirmResult(response.method(), response.approvedAt());
        } catch (RestClientResponseException e) {
            logOutbound(paymentKey, "APPROVE", requestPayload, e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED,
                    "토스 결제 승인에 실패했습니다: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public CancelResult cancel(String paymentKey, String reason, BigDecimal amount) {
        String requestPayload = objectMapper.writeValueAsString(new TossCancelRequest(reason, toKrwAmount(amount)));

        try {
            String responsePayload = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new TossCancelRequest(reason, toKrwAmount(amount)))
                    .retrieve()
                    .body(String.class);

            logOutbound(paymentKey, "CANCEL", requestPayload, responsePayload);
            TossCancelResponse response = objectMapper.readValue(responsePayload, TossCancelResponse.class);

            return new CancelResult(response.latestCancelledAt());
        } catch (RestClientResponseException e) {
            logOutbound(paymentKey, "CANCEL", requestPayload, e.getResponseBodyAsString());
            
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_CANCEL_FAILED,
                    "토스 결제취소에 실패했습니다: " + e.getResponseBodyAsString());
        }
    }

    /** 감사 로그 저장. 실패해도 삼켜서 실제 결제/환불 흐름을 방해하지 않는다. */
    private void logOutbound(String paymentKey, String action, String requestPayload, String responsePayload) {
        try {
            paymentLogRepository.save(paymentKey, action, requestPayload, responsePayload, null, OffsetDateTime.now(clock));
        } catch (Exception e) {
            log.warn("payment_logs 저장 실패 (paymentKey={}, action={})", paymentKey, action, e);
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
