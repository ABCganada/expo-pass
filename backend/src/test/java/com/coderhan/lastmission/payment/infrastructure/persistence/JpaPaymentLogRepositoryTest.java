package com.coderhan.lastmission.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaPaymentLogRepositoryTest {

    @Mock PaymentLogJpaRepository jpaRepository;

    @InjectMocks JpaPaymentLogRepository repository;

    @Test
    @DisplayName("orderId로 실제 승인 완료된 APPROVE 기록이 있으면 true를 반환한다")
    void returnsTrueWhenApprovedLogExists() {
        when(jpaRepository.existsApprovedOrderId("ORD-1")).thenReturn(true);

        assertThat(repository.existsApprovedOrderId("ORD-1")).isTrue();
    }

    @Test
    @DisplayName("일치하는 승인 완료 기록이 없으면 false를 반환한다")
    void returnsFalseWhenNoApprovedLogExists() {
        when(jpaRepository.existsApprovedOrderId("ORD-1")).thenReturn(false);

        assertThat(repository.existsApprovedOrderId("ORD-1")).isFalse();
    }
}
