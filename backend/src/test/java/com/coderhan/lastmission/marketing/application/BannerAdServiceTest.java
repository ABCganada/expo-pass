package com.coderhan.lastmission.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.coderhan.lastmission.marketing.domain.BannerAd;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BannerAdServiceTest {
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID AD_ID = UUID.randomUUID();
    private static final Set<UUID> SLOT_IDS = Set.of(SLOT_ID);
    private static final long MARKETER = 1001L;
    private static final long OTHER = 9999L;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-28T00:00:00Z");
    private static final OffsetDateTime STARTS_AT = NOW.plusDays(1);
    private static final OffsetDateTime ENDS_AT = NOW.plusDays(8); // 7일
    private static final long PRICE_PER_DAY = 30_000L; // 7일 × 30,000 = 210,000

    @Mock BannerAdRepository adRepository;
    @Mock BannerSlotRepository slotRepository;
    @Mock BannerStatRepository statRepository;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-28T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks BannerAdService service;

    // ─────────────────────────────────────────────────────────────────────────
    // registerAd
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void registerAd_슬롯이_비어있으면_예외() {
        // Act & Assert
        assertThatThrownBy(() -> service.registerAd(
                Set.of(), "여름 세일", "https://img.example.com/banner.png", null,
                null, STARTS_AT, ENDS_AT, MARKETER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_INVALID_REQUEST));

        verify(adRepository, never()).save(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void registerAd_존재하지_않는_슬롯이면_예외() {
        // Arrange
        when(slotRepository.findAllByIds(SLOT_IDS)).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> service.registerAd(
                SLOT_IDS, "여름 세일", "https://img.example.com/banner.png", null,
                null, STARTS_AT, ENDS_AT, MARKETER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_SLOT_NOT_FOUND));
    }

    @Test
    void registerAd_제목이_없으면_예외() {
        // Arrange
        when(slotRepository.findAllByIds(SLOT_IDS)).thenReturn(List.of(bannerSlot()));

        // Act & Assert
        assertThatThrownBy(() -> service.registerAd(
                SLOT_IDS, "", "https://img.example.com/banner.png", null,
                null, STARTS_AT, ENDS_AT, MARKETER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_INVALID_REQUEST));
    }

    @Test
    void registerAd_종료일이_시작일보다_이전이면_예외() {
        // Arrange
        when(slotRepository.findAllByIds(SLOT_IDS)).thenReturn(List.of(bannerSlot()));

        // Act & Assert
        assertThatThrownBy(() -> service.registerAd(
                SLOT_IDS, "여름 세일", "https://img.example.com/banner.png", null,
                null, ENDS_AT, STARTS_AT, MARKETER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_INVALID_REQUEST));
    }

    @Test
    void registerAd_배너슬롯인데_배너이미지_없으면_예외() {
        // Arrange
        when(slotRepository.findAllByIds(SLOT_IDS)).thenReturn(List.of(bannerSlot()));

        // Act & Assert
        assertThatThrownBy(() -> service.registerAd(
                SLOT_IDS, "여름 세일", null, null,
                null, STARTS_AT, ENDS_AT, MARKETER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_INVALID_REQUEST));
    }

    @Test
    void registerAd_정상입력이면_pricePerDay_곱셈으로_금액_계산_후_PENDING_저장() {
        // Arrange — 배너슬롯 30,000/일 × 7일 = 210,000
        long expectedAmount = PRICE_PER_DAY * 7;
        BannerAd expected = ad(BannerAdStatus.PENDING, expectedAmount);
        when(slotRepository.findAllByIds(SLOT_IDS)).thenReturn(List.of(bannerSlot()));
        when(adRepository.save(eq(SLOT_IDS), anyString(), eq("여름 세일"), eq("https://img.example.com/banner.png"),
                isNull(), isNull(), eq(STARTS_AT), eq(ENDS_AT), eq(MARKETER), eq(expectedAmount))).thenReturn(expected);


        // Act
        BannerAd result = service.registerAd(
                SLOT_IDS, "여름 세일", "https://img.example.com/banner.png", null,
                null, STARTS_AT, ENDS_AT, MARKETER);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(result.status()).isEqualTo(BannerAdStatus.PENDING);
        assertThat(result.totalAmount()).isEqualTo(expectedAmount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAd
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateAd_광고가_없으면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.updateAd(
                AD_ID, MARKETER, "수정된 제목", "https://img.example.com/banner.png", null, null, STARTS_AT, ENDS_AT))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_NOT_FOUND));
    }

    @Test
    void updateAd_본인_광고가_아니면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.PENDING)));

        // Act & Assert
        assertThatThrownBy(() -> service.updateAd(
                AD_ID, OTHER, "수정된 제목", "https://img.example.com/banner.png", null, null, STARTS_AT, ENDS_AT))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_ACCESS_DENIED));
    }

    @Test
    void updateAd_이미_처리된_광고는_수정_불가() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.APPROVED)));

        // Act & Assert
        assertThatThrownBy(() -> service.updateAd(
                AD_ID, MARKETER, "수정된 제목", "https://img.example.com/banner.png", null, null, STARTS_AT, ENDS_AT))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_ALREADY_REVIEWED));
    }

    @Test
    void updateAd_PENDING이고_본인이면_수정_성공() {
        // Arrange
        BannerAd updated = ad(BannerAdStatus.PENDING);
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.PENDING)));
        when(adRepository.update(AD_ID, "수정된 제목", "https://img.example.com/banner.png",
                null, null, STARTS_AT, ENDS_AT)).thenReturn(updated);

        // Act
        BannerAd result = service.updateAd(
                AD_ID, MARKETER, "수정된 제목", "https://img.example.com/banner.png", null, null, STARTS_AT, ENDS_AT);

        // Assert
        assertThat(result).isSameAs(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // approve
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void approve_광고가_없으면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.approve(AD_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_NOT_FOUND));
    }

    @Test
    void approve_이미_처리된_광고면_예외() {
        // Arrange
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.APPROVED)));

        // Act & Assert
        assertThatThrownBy(() -> service.approve(AD_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_AD_ALREADY_REVIEWED));
    }

    @Test
    void approve_PENDING_광고면_APPROVED로_변경() {
        // Arrange
        BannerAd approved = ad(BannerAdStatus.APPROVED);
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.PENDING)));
        when(adRepository.updateStatus(AD_ID, BannerAdStatus.APPROVED)).thenReturn(approved);

        // Act
        BannerAd result = service.approve(AD_ID);

        // Assert
        assertThat(result.status()).isEqualTo(BannerAdStatus.APPROVED);
        verify(adRepository).updateStatus(AD_ID, BannerAdStatus.APPROVED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reject
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void reject_PENDING_광고면_REJECTED로_변경() {
        // Arrange
        BannerAd rejected = ad(BannerAdStatus.REJECTED);
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(ad(BannerAdStatus.PENDING)));
        when(adRepository.updateStatus(AD_ID, BannerAdStatus.REJECTED)).thenReturn(rejected);

        // Act
        BannerAd result = service.reject(AD_ID);

        // Assert
        assertThat(result.status()).isEqualTo(BannerAdStatus.REJECTED);
        verify(adRepository).updateStatus(AD_ID, BannerAdStatus.REJECTED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveBanners
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getActiveBanners_현재_시각_기준으로_활성_광고_반환() {
        // Arrange
        List<BannerAd> active = List.of(ad(BannerAdStatus.APPROVED));
        when(adRepository.findAllActive(NOW)).thenReturn(active);

        // Act
        List<BannerAd> result = service.getActiveBanners();

        // Assert
        assertThat(result).isSameAs(active);
        verify(adRepository).findAllActive(NOW);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BannerSlot bannerSlot() {
        return new BannerSlot(SLOT_ID, "메인 배너", 3, BannerSlotType.BANNER, PRICE_PER_DAY, NOW);
    }

    private BannerAd ad(BannerAdStatus status) {
        return new BannerAd(AD_ID, SLOT_IDS, null, "여름 세일",
                "https://img.example.com/banner.png", null,
                null, status, STARTS_AT, ENDS_AT, MARKETER, NOW, null);
    }

    private BannerAd ad(BannerAdStatus status, long totalAmount) {
        return new BannerAd(AD_ID, SLOT_IDS, null, "여름 세일",
                "https://img.example.com/banner.png", null,
                null, status, STARTS_AT, ENDS_AT, MARKETER, NOW, totalAmount);
    }
}
