package com.coderhan.lastmission.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStats;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BannerStatServiceTest {
    private static final UUID AD_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 28);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-28T00:00:00Z");

    @Mock BannerStatRepository statRepository;
    @Mock BannerAdRepository adRepository;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-28T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks BannerStatService service;

    // ─────────────────────────────────────────────────────────────────────────
    // recordImpression
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recordImpression_광고가_없으면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.recordImpression(AD_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_NOT_FOUND));

        verify(statRepository, never()).incrementImpression(AD_ID, TODAY);
    }

    @Test
    void recordImpression_정상_호출() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad()));

        // Act
        service.recordImpression(AD_ID);

        // Assert
        verify(statRepository).incrementImpression(AD_ID, TODAY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // recordClick
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recordClick_광고가_없으면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.recordClick(AD_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_NOT_FOUND));

        verify(statRepository, never()).incrementClick(AD_ID, TODAY);
    }

    @Test
    void recordClick_정상_호출() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad()));

        // Act
        service.recordClick(AD_ID);

        // Assert
        verify(statRepository).incrementClick(AD_ID, TODAY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getStats_광고가_없으면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getStats(AD_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_NOT_FOUND));
    }

    @Test
    void getStats_노출이_없으면_CTR은_0() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad()));
        when(statRepository.sumStats(AD_ID)).thenReturn(new BannerAdStats(AD_ID, 0, 0, 0.0));

        // Act
        BannerAdStats result = service.getStats(AD_ID);

        // Assert
        assertThat(result.impressions()).isZero();
        assertThat(result.clicks()).isZero();
        assertThat(result.ctr()).isZero();
    }

    @Test
    void getStats_정상_통계_반환() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad()));
        when(statRepository.sumStats(AD_ID)).thenReturn(new BannerAdStats(AD_ID, 1000, 50, 5.0));

        // Act
        BannerAdStats result = service.getStats(AD_ID);

        // Assert
        assertThat(result.impressions()).isEqualTo(1000);
        assertThat(result.clicks()).isEqualTo(50);
        assertThat(result.ctr()).isEqualTo(5.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BannerAd ad() {
        return new BannerAd(AD_ID, Set.of(UUID.randomUUID()), "여름 세일", "https://img.example.com/img.png",
                null, null, 1, BannerAdStatus.APPROVED, NOW.plusDays(1), NOW.plusDays(30),
                "marketer@example.com", NOW, null);
    }
}
