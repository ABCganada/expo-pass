package com.coderhan.lastmission.user.domain;

public enum RoleCode {
    ADMIN, MANAGER, USER, DEVELOPER;

    public static RoleCode from(String value) {
        return RoleCode.valueOf(value.toUpperCase());
    }
}
