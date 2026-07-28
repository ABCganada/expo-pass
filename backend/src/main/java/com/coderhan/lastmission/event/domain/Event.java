package com.coderhan.lastmission.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @ManyToOne
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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
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

    public void publish() {
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        this.status = EventStatus.CANCELLED;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}