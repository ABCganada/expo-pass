package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.coderhan.lastmission.marketing.domain.BannerSlotType;

@Entity
@Table(name = "marketing_banner_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class BannerSlotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "max_count", nullable = false)
    private int maxCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BannerSlotType type;

    @Column(name = "price_per_day", nullable = false)
    private long pricePerDay;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
