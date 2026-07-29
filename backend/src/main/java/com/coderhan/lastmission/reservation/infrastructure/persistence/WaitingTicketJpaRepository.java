package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.reservation.domain.WaitingTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WaitingTicketJpaRepository extends JpaRepository<WaitingTicketEntity, Long> {

    // "event_id, user_id로 조회"하는 쿼리. 만료된 티켓은 지우지 않고 새로 재입장하면 새 row가
    // 쌓이는 구조라, 이 유저의 티켓이 여러 개일 수 있음 — 그중 가장 최근(ticketNo가 가장 큰) 것만 가져온다.
    Optional<WaitingTicketEntity> findFirstByEventIdAndUserIdOrderByTicketNoDesc(Long eventId, Long userId);

    // "이 행사에서, 이 상태(WAITING)이고, 내 ticketNo보다 작은 것들의 개수" — 앞에 몇 명 있는지 세는 용도
    long countByEventIdAndStatusAndTicketNoLessThan(Long eventId, WaitingTicketStatus status, Long ticketNo);

    // WAITING → ADMITTED 조건부 전이. WHERE절에 status=WAITING 조건이 있어서
    // 이미 ADMITTED/USED/EXPIRED인 티켓엔 아무 영향 안 줌 (동시 요청에도 안전)
    @Modifying
    @Query("""
            UPDATE WaitingTicketEntity t
            SET t.status = com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.ADMITTED,
                t.admittedAt = :now
            WHERE t.ticketNo = :ticketNo
              AND t.status = com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.WAITING
            """)
    int admit(@Param("ticketNo") Long ticketNo, @Param("now") OffsetDateTime now);

    // ADMITTED → USED 조건부 전이. 이미 USED거나 아직 ADMITTED가 아니면 0행 영향(실패로 판단 가능)
    @Modifying
    @Query("""
            UPDATE WaitingTicketEntity t
            SET t.status = com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.USED
            WHERE t.ticketNo = :ticketNo
              AND t.status = com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.ADMITTED
            """)
    void markUsed(@Param("ticketNo") Long ticketNo);

    // 클라이언트가 상태를 폴링할 때마다 호출 — "아직 보고 있다"는 마지막 시각 갱신
    @Modifying
    @Query("UPDATE WaitingTicketEntity t SET t.lastPolledAt = :now WHERE t.ticketNo = :ticketNo")
    void touch(@Param("ticketNo") Long ticketNo, @Param("now") OffsetDateTime now);

    // 스케줄러가 주기적으로 호출 -> 폴링 끊긴 지 오래된(threshold보다 오래된) WAITING/ADMITTED 티켓을
    // 일괄로 EXPIRED 처리해서 재입장 가능하게 풀어줌
    @Modifying
    @Query("""
            UPDATE WaitingTicketEntity t
            SET t.status = com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.EXPIRED
            WHERE t.status IN (
                com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.WAITING,
                com.coderhan.lastmission.reservation.domain.WaitingTicketStatus.ADMITTED)
              AND t.lastPolledAt < :threshold
            """)
    int expireStale(@Param("threshold") OffsetDateTime threshold);
}