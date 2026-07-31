@org.springframework.modulith.ApplicationModule(
    displayName = "결제",
    // 사용자 식별, 공통 실시간 연결, 결제승인 시 주문 금액 검증(ReservationOrderDirectory),
    // 행사 종료 이벤트 구독(event.EventEndedEvent) 사용
    allowedDependencies = {"user", "reservation", "marketing", "event", "shared", "shared::error", "shared::realtime"}
)

package com.coderhan.lastmission.payment;