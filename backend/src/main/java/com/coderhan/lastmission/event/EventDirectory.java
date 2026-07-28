package com.coderhan.lastmission.event;

import java.util.Optional;

/**
 * 재고 조회 / 차감
 */
public interface EventDirectory {
    /** 활성 티켓 정보를 조회 */
    Optional<TicketInfo> getTicketInfo(long ticketId);

    /** 재고 차감 */
    boolean decreaseTicketStock(long ticketId, int quantity);
}