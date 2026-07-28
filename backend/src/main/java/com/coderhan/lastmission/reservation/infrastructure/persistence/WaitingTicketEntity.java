package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.reservation.domain.WaitingTicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_waiting_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WaitingTicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservationWaitingTicketSeqGen")
    @SequenceGenerator(
            name = "reservationWaitingTicketSeqGen",
            sequenceName = "reservation_waiting_ticket_seq",
            allocationSize = 1) // 1로 안 하면 Hibernate가 50개씩 미리 당겨써서 순서 보장이 깨진다
    @Column(name = "ticket_no")
    private Long ticketNo;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaitingTicketStatus status;

    @Column(name = "entered_at", nullable = false)
    private OffsetDateTime enteredAt;

    @Column(name = "admitted_at")
    private OffsetDateTime admittedAt;

    @Column(name = "last_polled_at", nullable = false)
    private OffsetDateTime lastPolledAt;

    WaitingTicketEntity(Long eventId, Long userId, OffsetDateTime now) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = WaitingTicketStatus.WAITING;
        this.enteredAt = now;
        this.lastPolledAt = now;
    }
}