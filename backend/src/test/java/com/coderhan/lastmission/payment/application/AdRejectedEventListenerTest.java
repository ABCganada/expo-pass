package com.coderhan.lastmission.payment.application;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.AdRejectedEvent;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdRejectedEventListenerTest {
    private static final UUID AD_ID = UUID.randomUUID();
    private static final String ORDER_ID = "AD-ORD-1";
    private static final long PAYMENT_ID = 5L;
    private static final long OWNER_ID = 1001L;

    @Mock PaymentRepository paymentRepository;
    @Mock RefundService refundService;

    @InjectMocks AdRejectedEventListener listener;

    @Test
    void 결제를_찾으면_결제_소유자로_전액_환불을_요청한다() {
        Payment payment = Payment.builder()
                .id(PAYMENT_ID)
                .orderId(ORDER_ID)
                .userId(OWNER_ID)
                .amount(BigDecimal.valueOf(210000))
                .status(PaymentStatus.COMPLETED)
                .build();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        listener.on(new AdRejectedEvent(AD_ID, ORDER_ID));

        verify(refundService).request(OWNER_ID, PAYMENT_ID, "관리자에 의해 광고가 반려되어 자동 환불되었습니다.");
    }

    @Test
    void 결제를_못_찾으면_환불을_요청하지_않는다() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        listener.on(new AdRejectedEvent(AD_ID, ORDER_ID));

        verify(refundService, never()).request(anyLong(), anyLong(), anyString());
    }
}
