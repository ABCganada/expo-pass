package com.coderhan.lastmission.event.domain;

/** 저장되지 않는 계산 값. {@link Event#phase(java.time.LocalDate)}로 조회 시점에 계산한다. */
public enum EventPhase {
    UPCOMING,
    ONGOING,
    ENDED
}