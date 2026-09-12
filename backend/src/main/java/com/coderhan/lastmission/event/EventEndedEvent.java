package com.coderhan.lastmission.event;

import java.time.LocalDate;

/**
 * 행사 종료(end_date 경과)를 알리는 도메인 이벤트.
 */
public record EventEndedEvent(long eventId, LocalDate endDate) {
}