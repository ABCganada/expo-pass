package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerSlot;

public interface BannerSlotRepository {
    BannerSlot save(String name, int maxCount);
    List<BannerSlot> findAll();
    Optional<BannerSlot> findById(UUID id);
}
