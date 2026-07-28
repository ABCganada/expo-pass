package com.coderhan.lastmission.reservation.domain;

import java.time.OffsetDateTime;

/**
 * 선착순 대기열 티켓 한 장.
 *
 * ticketNo 는 입장 순서를 결정하는 단조 증가 번호다
 *
 * <p>{@code lastPolledAt} 은 클라이언트가 마지막으로 상태를 조회한 시각이다. 이게 너무
 * 오래되면(폴링이 끊기면, 약 20~30초) 사용자가 나갔다고 보고 EXPIRED 로 전환한다.</p>
 *
 * <p>상태 전이: WAITING → ADMITTED(카프카 컨슈머가 순서대로 허가) → USED(주문 생성 시 소비)
 * / WAITING·ADMITTED → EXPIRED(폴링 끊김 감지, 재입장 가능해짐)</p>
 */
public record WaitingTicket(
        long ticketNo,
        long eventId,
        long userId,
        // WAITING, ADMITTED, USED, EXPIRED
        WaitingTicketStatus status,
        // 대기열 입장 시각
        OffsetDateTime enteredAt,
        // 허가(순서가 와서 주문 가능해진) 시각
        OffsetDateTime admittedAt,
        // 클라이언트가 마지막으로 상태 조회한 시각
        OffsetDateTime lastPolledAt
) {
}