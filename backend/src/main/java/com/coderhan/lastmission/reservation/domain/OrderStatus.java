package com.coderhan.lastmission.reservation.domain;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED, //취소는 금액을 아직 지불하지 않음
    REFUNDED   //환불은 금액을 지불한 상태
}
