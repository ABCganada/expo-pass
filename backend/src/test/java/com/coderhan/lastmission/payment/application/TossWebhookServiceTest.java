package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TossWebhookServiceTest {
    private static final String TRANSMISSION_ID = "txn-1";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock PaymentLogRepository paymentLogRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);
    private TossWebhookService service;

    @BeforeEach
    void setUp() {
        service = new TossWebhookService(paymentLogRepository, new ObjectMapper(), clock);
    }

    @Test
    @DisplayName("정상 페이로드면 eventType과 paymentKey를 뽑아 원본 body 그대로 저장한다")
    void savesLogWithParsedEventTypeAndPaymentKey() {
        String rawBody = """
                {"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2026-07-23T10:00:00.000000",
                 "data":{"paymentKey":"pg-tx-1","status":"DONE"}}""";

        service.receive(TRANSMISSION_ID, rawBody);

        verify(paymentLogRepository).save("pg-tx-1", "PAYMENT_STATUS_CHANGED", rawBody, null, TRANSMISSION_ID, NOW);
    }

    @Test
    @DisplayName("data가 없어도 저장은 하되 paymentKey는 null로 남긴다")
    void savesLogWithNullPaymentKeyWhenDataMissing() {
        String rawBody = "{\"eventType\":\"seller.changed\"}";

        service.receive(TRANSMISSION_ID, rawBody);

        verify(paymentLogRepository).save(null, "seller.changed", rawBody, null, TRANSMISSION_ID, NOW);
    }

    @Test
    @DisplayName("파싱 불가능한 body가 와도 예외를 던지지 않고 저장을 스킵한다")
    void doesNotThrowAndSkipsSaveWhenBodyIsMalformed() {
        assertThatCode(() -> service.receive(TRANSMISSION_ID, "이건 JSON이 아님"))
                .doesNotThrowAnyException();

        verify(paymentLogRepository, never()).save(any(), any(), any(), any(), any(), any());
    }
}
