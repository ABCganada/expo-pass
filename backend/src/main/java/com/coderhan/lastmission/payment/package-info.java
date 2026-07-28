@org.springframework.modulith.ApplicationModule(
    displayName = "결제",
    // 사용자 식별과 공통 실시간 연결만 사용한다.
    allowedDependencies = {"user", "shared", "shared::error", "shared::realtime"}
)

package com.coderhan.lastmission.payment;