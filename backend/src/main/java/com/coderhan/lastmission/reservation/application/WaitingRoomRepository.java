package com.coderhan.lastmission.reservation.application;

import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.reservation.domain.WaitingTicket;

public interface WaitingRoomRepository {
    /**
     * 새 대기 티켓을 발급한다. 이미 (eventId, userId) 로 WAITING/ADMITTED 티켓이 있으면
     * DuplicateKeyException 이 발생한다(호출부에서 캐치해 멱등 처리).
     */
    WaitingTicket enterQueue(long eventId, long userId, OffsetDateTime now);

    // 티켓 조회 — 이미 대기 중인지 확인할 때, 상태 조회할 때 둘 다 씀
    Optional<WaitingTicket> findTicket(long eventId, long userId);

    // 이 티켓보다 먼저 들어와서 아직 WAITING 인 인원 수 — "내 앞에 몇 명 있나" 계산용
    long countWaitingAhead(long eventId, long ticketNo);

    // WAITING → ADMITTED 조건부 전이. now 는 카프카 컨슈머가 Clock으로 만들어서 넘겨줌
    // (이 인터페이스 구현체가 직접 OffsetDateTime.now()를 호출하지 않게 — 테스트 시 시간 고정 가능하게)
    boolean admit(long ticketNo, OffsetDateTime now);

    // ADMITTED → USED 조건부 전이 — ReservationService.createOrder가 게이트 통과시킬 때 호출
    void markUsed(long ticketNo);

    // 상태 조회(폴링) 시마다 호출 — lastPolledAt 을 갱신해서 "아직 보고 있다"는 신호를 남김
    void touch(long ticketNo, OffsetDateTime now);

    // lastPolledAt 이 threshold 보다 오래된 WAITING/ADMITTED 티켓을 전부 EXPIRED 로 전환.
    // 스케줄러가 주기적으로 호출한다. 반환값은 만료 처리된 티켓 수(로깅용)
    int expireStale(OffsetDateTime threshold);
}