package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.reservation.application.WaitingRoomRepository;
import com.coderhan.lastmission.reservation.domain.WaitingTicket;
import com.coderhan.lastmission.reservation.domain.WaitingTicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 대기열 저장소. reservation_waiting_tickets 만 읽고 쓴다.
 * WaitingTicketJpaRepository(Spring Data)를 감싸서, Entity ↔ record 변환은 여기서만 한다.
 */
@Repository
@RequiredArgsConstructor
class JpaWaitingRoomRepository implements WaitingRoomRepository {
    private final WaitingTicketJpaRepository jpaRepository;

    @Override
    public WaitingTicket enterQueue(long eventId, long userId, OffsetDateTime now) {
        // save() 호출 시 ticketNo 는 @GeneratedValue(SEQUENCE)로 DB가 자동 채워줌
        WaitingTicketEntity saved = jpaRepository.save(new WaitingTicketEntity(eventId, userId, now));
        return toDomain(saved);
    }

    @Override
    public Optional<WaitingTicket> findTicket(long eventId, long userId) {
        return jpaRepository.findByEventIdAndUserId(eventId, userId).map(JpaWaitingRoomRepository::toDomain);
    }

    @Override
    public long countWaitingAhead(long eventId, long ticketNo) {
        return jpaRepository.countByEventIdAndStatusAndTicketNoLessThan(
                eventId, WaitingTicketStatus.WAITING, ticketNo);
    }

    @Override
    public boolean admit(long ticketNo, OffsetDateTime now) {
        // 영향받은 row 수가 0이면 "이미 ADMITTED거나 없는 티켓" → false로 알려줌
        return jpaRepository.admit(ticketNo, now) > 0;
    }

    @Override
    public void markUsed(long ticketNo) {
        jpaRepository.markUsed(ticketNo);
    }

    @Override
    public void touch(long ticketNo, OffsetDateTime now) {
        jpaRepository.touch(ticketNo, now);
    }

    @Override
    public int expireStale(OffsetDateTime threshold) {
        return jpaRepository.expireStale(threshold);
    }

    // Entity(JPA) → WaitingTicket(순수 record) 변환. 이 클래스 밖으로는 Entity가 절대 안 나간다.
    private static WaitingTicket toDomain(WaitingTicketEntity entity) {
        return new WaitingTicket(entity.getTicketNo(), entity.getEventId(), entity.getUserId(),
                entity.getStatus(), entity.getEnteredAt(), entity.getAdmittedAt(), entity.getLastPolledAt());
    }
}