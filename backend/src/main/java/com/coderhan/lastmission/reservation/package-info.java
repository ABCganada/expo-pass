@org.springframework.modulith.ApplicationModule(
        displayName = "예약",
        // 사용자 식별(LastMissionPrincipal), 대기열 카프카 발행 후 커밋 이후 실행(AfterCommitExecutor)에 사용.
        allowedDependencies = {"user", "event", "shared", "shared::error", "shared::realtime"}
)
package com.coderhan.lastmission.reservation;
