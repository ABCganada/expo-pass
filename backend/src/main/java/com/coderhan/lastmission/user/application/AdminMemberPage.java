package com.coderhan.lastmission.user.application;

import java.util.List;

/** 회원 목록 한 페이지. */
public record AdminMemberPage(List<AdminMember> members, int page, int size, long totalElements) {
}
