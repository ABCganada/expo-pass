package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.marketing.application.BannerSlotRepository;
import com.coderhan.lastmission.marketing.domain.BannerSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaBannerSlotRepository implements BannerSlotRepository {
    private final BannerSlotJpaRepository jpaRepository;

    @Override
    public BannerSlot save(String name, int maxCount) {
        BannerSlotEntity saved = jpaRepository.save(
                new BannerSlotEntity(null, name, maxCount, OffsetDateTime.now()));
        return toDomain(saved);
    }

    @Override
    public List<BannerSlot> findAll() {
        return jpaRepository.findAll().stream()
                .map(JpaBannerSlotRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<BannerSlot> findById(UUID id) {
        return jpaRepository.findById(id).map(JpaBannerSlotRepository::toDomain);
    }

    private static BannerSlot toDomain(BannerSlotEntity entity) {
        return new BannerSlot(entity.getId(), entity.getName(), entity.getMaxCount(), entity.getCreatedAt());
    }
}
