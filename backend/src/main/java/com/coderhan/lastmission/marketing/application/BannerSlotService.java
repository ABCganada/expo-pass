package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;
import com.coderhan.lastmission.marketing.application.BannerAdRepository;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerSlotService {
    private final BannerSlotRepository slotRepository;
    private final BannerAdRepository adRepository;

    @Transactional
    public BannerSlot createSlot(String name, int maxCount, BannerSlotType type) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 이름은 필수입니다.");
        }
        if (maxCount < 1) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 최대 광고 수는 1 이상이어야 합니다.");
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 타입은 필수입니다.");
        }
        return slotRepository.save(name, maxCount, type);
    }

    @Transactional(readOnly = true)
    public List<BannerSlot> getSlots() {
        return slotRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BannerSlot getSlot(UUID id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "광고 슬롯을 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public BannerSlot updateSlot(UUID id, String name, int maxCount, BannerSlotType type) {
        slotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "광고 슬롯을 찾을 수 없습니다. id=" + id));
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 이름은 필수입니다.");
        }
        if (maxCount < 1) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 최대 광고 수는 1 이상이어야 합니다.");
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "슬롯 타입은 필수입니다.");
        }
        return slotRepository.update(id, name, maxCount, type);
    }

    @Transactional
    public void deleteSlot(UUID id) {
        slotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "광고 슬롯을 찾을 수 없습니다. id=" + id));
        if (adRepository.existsActiveOrPendingBySlotId(id)) {
            throw new BusinessException(ErrorCode.BANNER_SLOT_HAS_ACTIVE_ADS,
                    "진행 중이거나 검토 중인 광고가 있는 슬롯은 삭제할 수 없습니다.");
        }
        slotRepository.deleteById(id);
    }
}
