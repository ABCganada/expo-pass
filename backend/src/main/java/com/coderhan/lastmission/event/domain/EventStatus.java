package com.coderhan.lastmission.event.domain;

/** DB에 저장되는 행사 상태. UPCOMING/ONGOING/ENDED는 저장하지 않고 조회 시 계산한다({@link EventPhase}). */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED
}