package com.coderhan.lastmission.event;

public interface ReservationQueryPort {

    /** 이 행사에 유효한 예약이 하나라도 있는지 확인 */
    boolean hasActiveReservationsForEvent(long eventId);

    /** 이 티켓에 유효한(취소/환불되지 않은) 예약이 하나라도 있는지 확인 */
    boolean hasActiveReservationsForTicket(long ticketId);
}