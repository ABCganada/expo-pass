package com.coderhan.lastmission.payment.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * 토스페이먼츠 웹훅 페이로드를 파싱해 payment_logs에 감사 로그로 남긴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossWebhookService {

    private final PaymentLogRepository paymentLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 웹훅 수신 처리. 파싱/저장 중 무엇이 실패해도 예외를 밖으로 던지지 않는다 — 컨트롤러가
     * 항상 200을 줄 수 있어야 하기 때문이다(토스 10초 룰. 재시도로 해결될 문제가 아니면
     * 재시도만 반복시키는 꼴이라 의미가 없다).
     */
    public void receive(String transmissionId, String rawBody) {
        try {
            TossWebhookPayload payload = objectMapper.readValue(rawBody, TossWebhookPayload.class);
            String paymentKey = payload.data() != null ? payload.data().paymentKey() : null;

            paymentLogRepository.save(paymentKey, payload.eventType(), rawBody, null, transmissionId,
                    OffsetDateTime.now(clock));
        } catch (Exception e) {
            log.warn("토스 웹훅 처리 실패 (transmissionId={}, body={})", transmissionId, rawBody, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossWebhookPayload(String eventType, PaymentData data) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record PaymentData(String paymentKey) {}
    }
}
