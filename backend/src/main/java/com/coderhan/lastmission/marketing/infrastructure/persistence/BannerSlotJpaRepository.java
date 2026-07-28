package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BannerSlotJpaRepository extends JpaRepository<BannerSlotEntity, UUID> {
}
