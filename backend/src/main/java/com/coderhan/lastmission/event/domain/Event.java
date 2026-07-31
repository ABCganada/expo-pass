package com.coderhan.lastmission.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "address")
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "kakao_place_id")
    private String kakaoPlaceId;

    @Column(name = "legal_dong_code", length = 10)
    private String legalDongCode;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "ended_notified_at")
    private Instant endedNotifiedAt;

    public Event(String title, EventCategory category, Long managerId) {
        this.title = title;
        this.category = category;
        this.managerId = managerId;
        this.status = EventStatus.DRAFT;
        this.viewCount = 0L;
    }

    public EventPhase phase(LocalDate today) {
        if (today.isBefore(startDate)) return EventPhase.UPCOMING;
        if (today.isAfter(endDate)) return EventPhase.ENDED;
        return EventPhase.ONGOING;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 필드 수정 전용 */
    public void updateDetails(String title, EventCategory category, String hostName, String venueName,
            String address, String detailAddress, String kakaoPlaceId, String legalDongCode,
            BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.category = category;
        this.hostName = hostName;
        this.venueName = venueName;
        this.address = address;
        this.detailAddress = detailAddress;
        this.kakaoPlaceId = kakaoPlaceId;
        this.legalDongCode = legalDongCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void publish() {
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        this.status = EventStatus.CANCELLED;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
    }

    public void markEndedNotified(Instant now) {
        this.endedNotifiedAt = now;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}