package com.coderhan.lastmission.user;

/** 다른 모듈에 넘기는 최소 사용자 정보. 내부 엔티티를 밖으로 새지 않게 한다. */
public record UserRef(long id, String email, String name) {
}
