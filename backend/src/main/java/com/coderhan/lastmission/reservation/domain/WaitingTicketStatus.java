package com.coderhan.lastmission.reservation.domain;

public enum WaitingTicketStatus {
    WAITING,
    ADMITTED,
    USED,
    EXPIRED   // 폴링이 끊겨서 나갔다고 판단된 상태
}