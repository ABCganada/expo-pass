package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import com.coderhan.lastmission.marketing.BannerOrderDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaBannerOrderDirectory implements BannerOrderDirectory {
    private final BannerAdJpaRepository bannerAdJpaRepository;

    @Override
    public Optional<BigDecimal> findOrderAmount(String orderId) {
        return bannerAdJpaRepository.findByOrderId(orderId)
                .map(entity -> entity.getTotalAmount() == null ? null : BigDecimal.valueOf(entity.getTotalAmount()));
    }
}
