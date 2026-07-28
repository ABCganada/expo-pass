package com.coderhan.lastmission.event.domain;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "event_contents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "content_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private EventContentType contentType;

    @Column(name = "content", nullable = false)
    private String content;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EventContent(Event event, EventContentType contentType, String content) {
        this.event = event;
        this.contentType = contentType;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}