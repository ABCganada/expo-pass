package com.coderhan.lastmission.user.application;

import java.util.Objects;

public record ProvisionUserCommand(String authSubject, String email, String name) {
    public ProvisionUserCommand {
        if (authSubject == null || authSubject.isBlank()) {
            throw new IllegalArgumentException("인증된 사용자의 subject가 필요합니다.");
        }
        email = Objects.requireNonNullElse(email, "");
        name = Objects.requireNonNullElse(name, "");
    }
}
