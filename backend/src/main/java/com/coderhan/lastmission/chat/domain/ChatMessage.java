package com.coderhan.lastmission.chat.domain;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;

/**
 * 채팅 메시지 한 건.
 *
 * <p>{@code senderName} 은 발신 시점의 표시 이름 스냅샷이다. 사용자가 이름을 바꿔도
 * 과거 대화의 표시는 그대로 남는다.</p>
 */
public record ChatMessage(
        long id,
        long roomId,
        long senderId,
        String senderName,
        String content,
        ChatMessageType messageType,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    public static final int MAX_LENGTH = 1000;

    /** 앞뒤 공백을 다듬고 길이를 확인한다. 빈 내용과 과도한 길이는 저장하지 않는다. */
    public static String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID, "메시지 내용이 비어 있습니다.");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID,
                    "메시지는 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 삭제된 메시지는 내용을 감추고 자리만 남긴다. */
    public String displayContent() {
        return isDeleted() ? "삭제된 메시지입니다." : content;
    }
}
