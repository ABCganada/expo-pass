package com.coderhan.lastmission.chat.domain;

/**
 * 채팅 스트림에 저장되는 메시지의 의미.
 *
 * <p>이 값은 이미 수행된 행위를 기록하고 표시 방식을 구분한다.
 * 메시지를 조회하는 것만으로 입장·퇴장 같은 행위를 다시 실행하지 않는다.</p>
 */
public enum ChatMessageType {
    USER,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_INVITED,
    MEMBER_KICKED,
    ROOM_RENAMED
}
