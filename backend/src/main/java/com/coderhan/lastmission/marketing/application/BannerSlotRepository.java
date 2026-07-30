package com.coderhan.lastmission.marketing.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;

public interface BannerSlotRepository {
    BannerSlot save(String name, int maxCount, BannerSlotType type);
    List<BannerSlot> findAll();
    Optional<BannerSlot> findById(UUID id);
    List<BannerSlot> findAllByIds(Set<UUID> ids);
    BannerSlot update(UUID id, String name, int maxCount, BannerSlotType type);
    void deleteById(UUID id);
}
