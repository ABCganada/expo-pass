package com.coderhan.lastmission.payment.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdSettlementServiceTest {
    private static final UUID AD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock AdSettlementRepository adSettlementRepository;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks AdSettlementService service;

    /** 정산 로직의 핵심: 광고 만료 이벤트가 넘겨준 총금액에 수수료 5%를 적용해 저장하는지. */
    @Test
    @DisplayName("광고 만료 시 전달받은 총금액에 수수료 5%를 적용해 정산을 저장한다")
    void calculatesCommissionAndSavesSettlement() {
        when(adSettlementRepository.existsByAdId(AD_ID)).thenReturn(false);

        service.create(AD_ID, 24000L);

        // 총액 24000, 수수료 = 24000 * 5% = 1200, 정산액 = 22800
        verify(adSettlementRepository).save(AD_ID, BigDecimal.valueOf(24000), new BigDecimal("5.00"),
                BigDecimal.valueOf(1200), BigDecimal.valueOf(22800), OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("이미 생성된 정산이 있으면 아무 것도 하지 않는다(광고 만료 이벤트 중복 발행/재처리 방어)")
    void doesNothingWhenSettlementAlreadyExists() {
        when(adSettlementRepository.existsByAdId(AD_ID)).thenReturn(true);

        service.create(AD_ID, 24000L);

        verify(adSettlementRepository, never()).save(any(), any(), any(), any(), any(), any());
    }
}
