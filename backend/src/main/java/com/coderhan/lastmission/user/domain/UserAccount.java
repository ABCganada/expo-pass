package com.coderhan.lastmission.user.domain;

import java.util.Objects;

public record UserAccount(long id, String authSubject, String email, String name, Status status) {
    public UserAccount {
        if (id <= 0) throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        if (authSubject == null || authSubject.isBlank()) throw new IllegalArgumentException("인증 subject가 필요합니다.");
        email = Objects.requireNonNullElse(email, "");
        name = Objects.requireNonNullElse(name, "");
        Objects.requireNonNull(status, "사용자 상태가 필요합니다.");
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public enum Status {
        ACTIVE, INACTIVE;

        public static Status from(String value) {
            return Status.valueOf(value.toUpperCase());
        }
    }
}
