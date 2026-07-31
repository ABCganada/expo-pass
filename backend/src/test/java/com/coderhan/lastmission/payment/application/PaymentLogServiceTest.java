package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.PaymentLog;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentLogServiceTest {

    @Mock PaymentLogRepository repository;

    @InjectMocks PaymentLogService service;

    @Test
    @DisplayName("리포지토리가 조회한 페이지를 그대로 반환한다")
    void returnsPageFromRepository() {
        PaymentLog log = PaymentLog.builder()
                .id(1L)
                .paymentKey("payment-key-1")
                .action("APPROVE")
                .requestPayload("{}")
                .responsePayload("{}")
                .webhookTransmissionId(null)
                .createdAt(OffsetDateTime.parse("2026-07-23T10:00:00Z"))
                .build();
        PaymentLogPage expected = new PaymentLogPage(List.of(log), 0, 20, 1L);
        when(repository.findAll(0, 20)).thenReturn(expected);

        PaymentLogPage result = service.list(0, 20);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하는 id면 리포지토리가 조회한 로그를 그대로 반환한다")
    void returnsLogFromRepositoryWhenFound() {
        PaymentLog log = PaymentLog.builder()
                .id(1L)
                .paymentKey("payment-key-1")
                .action("APPROVE")
                .requestPayload("{}")
                .responsePayload("{}")
                .webhookTransmissionId(null)
                .createdAt(OffsetDateTime.parse("2026-07-23T10:00:00Z"))
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(log));

        PaymentLog result = service.get(1L);

        assertThat(result).isSameAs(log);
    }

    @Test
    @DisplayName("존재하지 않는 id면 PAYMENT_LOG_NOT_FOUND 예외를 던진다")
    void throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_LOG_NOT_FOUND);
    }
}
