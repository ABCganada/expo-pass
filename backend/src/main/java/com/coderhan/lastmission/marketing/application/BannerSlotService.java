package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerSlotService {
    private final BannerSlotRepository slotRepository;

    @Transactional(readOnly = true)
    public List<BannerSlot> getSlots() {
        return slotRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BannerSlot getSlot(UUID id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND, "광고 슬롯을 찾을 수 없습니다. id=" + id));
    }
}
