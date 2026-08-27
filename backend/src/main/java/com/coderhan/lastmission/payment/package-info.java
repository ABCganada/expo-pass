@org.springframework.modulith.ApplicationModule(
    displayName = "결제",
    // 사용자 식별, 공통 실시간 연결, 결제승인/정산 시 주문 조회(ReservationOrderDirectory),
    // 행사 종료 이벤트 구독(event.EventEndedEvent), 정산 담당자 조회(PaymentEventQueryPort),
    // 주문 타입(shared::order)/결제 상태 전파 이벤트(shared::event) 사용
    allowedDependencies = {"user", "reservation", "marketing", "event", "shared", "shared::error", "shared::realtime",
            "shared::order", "shared::event"}
)

package com.coderhan.lastmission.payment;