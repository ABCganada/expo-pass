@org.springframework.modulith.ApplicationModule(
        displayName = "예약",
        // 사용자 식별(LastMissionPrincipal)만 사용한다.
        allowedDependencies = {"user", "shared", "shared::error"}
)
package com.coderhan.lastmission.reservation;
