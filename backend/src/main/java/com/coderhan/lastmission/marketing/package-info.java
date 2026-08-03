@org.springframework.modulith.ApplicationModule(
        displayName = "마케팅",
        allowedDependencies = {"user", "shared", "shared::error", "shared::event", "shared::order", "payment"}
)
package com.coderhan.lastmission.marketing;
