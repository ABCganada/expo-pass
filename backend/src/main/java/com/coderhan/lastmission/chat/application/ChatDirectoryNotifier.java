package com.coderhan.lastmission.chat.application;

import java.util.Collection;

/**
 * 채팅방 목록·마지막 메시지·안 읽은 수가 바뀌었다는 신호를 전달한다.
 *
 * <p>신호에는 채팅 내용이나 사용자 정보가 없으며, 수신자는 REST로 자신의 최신 목록을
 * 다시 조회한다.</p>
 */
public interface ChatDirectoryNotifier {

    /** 공개방 생성처럼 모든 로그인 사용자의 공개방 목록이 바뀌는 경우. */
    void publicDirectoryChanged();

    /** 메시지·초대·읽음 처리처럼 지정된 사용자들의 목록만 바뀌는 경우. */
    void usersChanged(Collection<Long> userIds);

    default void userChanged(long userId) {
        usersChanged(java.util.List.of(userId));
    }
}
