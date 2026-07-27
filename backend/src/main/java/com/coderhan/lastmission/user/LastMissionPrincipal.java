package com.coderhan.lastmission.user;

public record LastMissionPrincipal(long userId, String authSubject, String email, String name) {
}
