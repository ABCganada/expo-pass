package com.coderhan.lastmission.reservation.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.reservation.domain.WaitingTicket;
import com.coderhan.lastmission.reservation.domain.WaitingTicketStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WaitingRoomService {
    private final WaitingRoomRepository repository;
    private final WaitingRoomEventPublisher eventPublisher;
    private final Clock clock;

    /** 대기열 입장. 이미 대기 중이면(중복 클릭 등) 새로 안 만들고 기존 티켓을 그대로 돌려준다(멱등). */
    @Transactional
    public WaitingTicket enterQueue(long userId, long eventId) {
        if (eventId <= 0) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "행사 ID가 올바르지 않습니다.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        try {
            WaitingTicket ticket = repository.enterQueue(eventId, userId, now);
            // 저장 성공 시에만 카프카 발행 (커밋 이후로 예약되는 건 Publisher 내부에서 처리)
            eventPublisher.publishJoined(ticket.ticketNo(), eventId, userId);
            return ticket;
        } catch (DuplicateKeyException e) {
            // 유니크 제약(event_id, user_id) 위반 = 이미 WAITING/ADMITTED 티켓이 있다는 뜻
            // → 새로 만들지 않고 기존 티켓을 찾아서 그대로 반환 (재클릭해도 안전)
            return repository.findTicket(eventId, userId).orElseThrow(() -> e);
        }
    }

    /** 상태 조회(폴링). 호출될 때마다 "아직 보고 있다"는 신호(lastPolledAt)를 갱신한다. */
    @Transactional
    public StatusResult getStatus(long userId, long eventId) {
        WaitingTicket ticket = repository.findTicket(eventId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESERVATION_WAITING_ROOM_REQUIRED, "대기열에 입장한 적이 없습니다."));

        repository.touch(ticket.ticketNo(), OffsetDateTime.now(clock));

        // WAITING 일 때만 "앞에 몇 명"이 의미 있음. ADMITTED/USED/EXPIRED면 순번 정보 필요 없음
        Long position = ticket.status() == WaitingTicketStatus.WAITING
                ? repository.countWaitingAhead(eventId, ticket.ticketNo())
                : null;
        return new StatusResult(ticket.status(), position);
    }

    /**
     * ReservationService.createOrder 가 호출하는 게이트. ADMITTED 상태가 아니면 주문 자체를 막는다.
     * 통과 시 티켓을 USED 로 전환해서, 이 허가로는 딱 한 번만 주문할 수 있게 소비한다.
     */
    @Transactional
    public void consumeAdmission(long userId, long eventId) {
        WaitingTicket ticket = repository.findTicket(eventId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESERVATION_WAITING_ROOM_REQUIRED, "먼저 대기열에 입장해야 합니다."));
        if (ticket.status() != WaitingTicketStatus.ADMITTED) {
            throw new BusinessException(
                    ErrorCode.RESERVATION_WAITING_ROOM_REQUIRED, "아직 입장 순서가 되지 않았습니다.");
        }
        repository.markUsed(ticket.ticketNo());
    }

    /** 30초마다, 폴링이 30초 넘게 끊긴 WAITING/ADMITTED 티켓을 EXPIRED 로 정리한다. */
    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void expireStaleTickets() {
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusSeconds(30);
        int expired = repository.expireStale(threshold);
        if (expired > 0) {
            log.info("폴링 끊긴 대기 티켓 {}건 만료 처리", expired);
        }
    }

    public record StatusResult(WaitingTicketStatus status, Long position) {}
}