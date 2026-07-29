package com.coderhan.lastmission.event;

import java.util.Optional;

/**
 * 재고 조회 / 차감 / 복원
 */
public interface EventQueryPort {
    /** 활성 티켓 정보 조회 */
    Optional<TicketInfo> getTicketInfo(long ticketId);

    /** 재고 차감 */
    boolean decreaseTicketStock(long ticketId, int quantity);

    /** 재고 복원 */
    void increaseTicketStock(long ticketId, int quantity);
}