package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerPricingPolicy;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerPricingPolicyService {
    private final BannerPricingPolicyRepository policyRepository;
    private final BannerSlotRepository slotRepository;

    @Transactional
    public BannerPricingPolicy createPolicy(UUID slotId, int durationDays, long price) {
        slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_SLOT_NOT_FOUND,
                        "슬롯을 찾을 수 없습니다. id=" + slotId));
        if (durationDays <= 0) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "기간은 1일 이상이어야 합니다.");
        }
        if (price < 0) {
            throw new BusinessException(ErrorCode.BANNER_AD_INVALID_REQUEST, "가격은 0 이상이어야 합니다.");
        }
        return policyRepository.save(slotId, durationDays, price);
    }

    @Transactional(readOnly = true)
    public List<BannerPricingPolicy> getPoliciesBySlot(UUID slotId) {
        return policyRepository.findBySlotId(slotId);
    }

    @Transactional
    public void deletePolicy(UUID id) {
        policyRepository.deleteById(id);
    }
}
