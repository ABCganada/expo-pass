package com.coderhan.lastmission.shared.event;

import java.time.LocalDate;

/**
 * DRAFT — 행사 종료 사실을 나타내는 도메인 이벤트. 패키지/필드/발행 시점 모두 Event 도메인
 * 담당자 확정 전까지의 잠정안이다(요청: 신민기 → 김수연).
 *
 * Event 도메인이 종료 감지 시점에 이 이벤트를 발행
 * TODO Event 도메인 담당자 구현 필요.
 */
public record EventEndedEvent(long eventId, LocalDate endDate) {
}
