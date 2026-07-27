package com.coderhan.lastmission.chat.domain;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 채팅방.
 *
 * <p>{@code isPublic} 이 true 면 공개방(누구나 읽고 쓴다, 생성은 관리자만),
 * false 면 비공개방(초대된 사람만, 생성은 누구나). {@code ownerUserId} 는 만든 사람이다.</p>
 *
 * <p>접근 판정은 도메인이 아니라 서비스가 한다 — 공개방은 무조건 통과지만, 비공개방은
 * "현재 참가자인가"를 참가자 표에서 확인해야 하므로 방 데이터만으로는 결정되지 않는다.</p>
 */
public record ChatRoom(
        long id,
        boolean isPublic,
        long ownerUserId,
        String name,
        boolean active,
        OffsetDateTime createdAt
) {
    public static final int MAX_NAME_LENGTH = 60;

    /** 방 이름을 다듬고 확인한다. 비었거나 너무 길면 거절. */
    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID, "방 이름을 입력하세요.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID,
                    "방 이름은 " + MAX_NAME_LENGTH + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }
}
