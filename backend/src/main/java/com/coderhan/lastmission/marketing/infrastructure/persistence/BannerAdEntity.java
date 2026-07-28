package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
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
import lombok.Setter;

@Entity
@Table(name = "marketing_banner_ads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class BannerAdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Setter
    @Column(name = "status", nullable = false)
    private BannerAdStatus status;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    void update(String title, String imageUrl, String linkUrl, int priority,
                OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.priority = priority;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
