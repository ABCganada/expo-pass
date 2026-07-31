package com.coderhan.lastmission.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BannerSlotServiceTest {
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-28T00:00:00Z");

    @Mock BannerSlotRepository slotRepository;

    @InjectMocks BannerSlotService service;

    // ─────────────────────────────────────────────────────────────────────────
    // getSlots
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSlots_전체_슬롯_목록_반환() {
        // Arrange
        List<BannerSlot> slots = List.of(
                new BannerSlot(SLOT_ID, "메인 배너", 3, BannerSlotType.BANNER, 50_000L, NOW),
                new BannerSlot(UUID.randomUUID(), "광고 탭", 5, BannerSlotType.TAB, 30_000L, NOW)
        );
        when(slotRepository.findAll()).thenReturn(slots);

        // Act
        List<BannerSlot> result = service.getSlots();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isSameAs(slots);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSlot
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getSlot_존재하는_슬롯이면_반환() {
        // Arrange
        BannerSlot expected = new BannerSlot(SLOT_ID, "메인 배너", 3, BannerSlotType.BANNER, 50_000L, NOW);
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(expected));

        // Act
        BannerSlot result = service.getSlot(SLOT_ID);

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getSlot_존재하지_않는_슬롯이면_예외() {
        // Arrange
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getSlot(SLOT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.BANNER_SLOT_NOT_FOUND));
    }
}
