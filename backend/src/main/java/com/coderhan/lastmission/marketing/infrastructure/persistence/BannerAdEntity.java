package com.coderhan.lastmission.marketing.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.coderhan.lastmission.marketing.domain.BannerAdStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    // N:M — 하나의 광고가 여러 슬롯에 노출될 수 있음
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "marketing_banner_ad_slots",
            joinColumns = @JoinColumn(name = "ad_id")
    )
    @Column(name = "slot_id")
    private Set<UUID> slotIds = new HashSet<>();

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "ad_image_url")
    private String adImageUrl;

    @Column(name = "link_url")
    private String linkUrl;

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

    // 등록 시점에 확정 (슬롯 단가 × 기간). null은 레거시 데이터만 해당.
    @Setter
    @Column(name = "total_amount")
    private Long totalAmount;

    void update(String title, String bannerImageUrl, String adImageUrl, String linkUrl,
                OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.title = title;
        this.bannerImageUrl = bannerImageUrl;
        this.adImageUrl = adImageUrl;
        this.linkUrl = linkUrl;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
