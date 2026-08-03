@org.springframework.modulith.ApplicationModule(
        displayName = "예약",
        // 사용자 식별(LastMissionPrincipal), 대기열 카프카 발행 후 커밋 이후 실행(AfterCommitExecutor)에 사용.
        // shared::event/shared::order는 결제 승인 이벤트(PaymentConfirmedEvent)를 구독해
        // PENDING 주문을 CONFIRMED로 전환하는 데 사용(payment가 reservation에 의존하므로
        // 반대로 payment 모듈을 직접 참조할 수 없어 shared 이벤트 타입으로 받는다).
        allowedDependencies = {"user", "event", "shared", "shared::error", "shared::realtime",
                "shared::event", "shared::order"}
)
package com.coderhan.lastmission.reservation;
