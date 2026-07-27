package com.coderhan.lastmission.chat.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.chat.domain.ChatRoom;

public interface ChatRepository {
    Optional<ChatRoom> findRoom(long roomId);

    /** 활성 공개방 전체. 공개방은 읽음 위치가 한 번 생긴 사용자만 안읽음을 센다. */
    List<RoomSummary> findPublicRooms(long userId);

    /** 현재 참가 중인 활성 비공개방만 반환한다. */
    List<RoomSummary> findPrivateRooms(long userId);

    ChatRoom createRoom(long ownerUserId, String name, boolean isPublic, OffsetDateTime now);

    boolean isCurrentParticipant(long roomId, long userId);

    List<Long> findCurrentParticipantIds(long roomId);

    Optional<OffsetDateTime> findCurrentJoinedAt(long roomId, long userId);

    void addParticipant(long roomId, long userId, OffsetDateTime joinedAt);

    boolean leaveParticipant(long roomId, long userId, OffsetDateTime leftAt);

    /**
     * 방의 메시지를 최신 것부터 {@code limit} 건 읽는다.
     * {@code beforeId} 가 있으면 그보다 오래된 것만 읽는다(위로 스크롤).
     * 비공개방은 현재 참여 구간의 {@code visibleAfter} 이후 메시지만 읽는다.
     */
    List<ChatMessage> findMessages(long roomId, OffsetDateTime visibleAfter, Long beforeId, int limit);

    ChatMessage save(long roomId, long senderId, String senderName, String content, OffsetDateTime sentAt);

    ChatMessage saveSystemEvent(
            long roomId, long actorUserId, ChatMessageType messageType,
            String content, OffsetDateTime occurredAt);

    /** 방의 가장 최근 메시지 ID. 없으면 0. */
    long findLatestMessageId(long roomId);

    /**
     * 이 방에서 사용자가 아직 안 읽은 메시지 수.
     * 공개방은 {@code requireReadMarker}가 true라 읽음 위치가 없으면 0을 반환한다.
     */
    int countUnread(long roomId, long userId, OffsetDateTime visibleAfter, boolean requireReadMarker);

    void markRead(long roomId, long userId, long lastReadMessageId, OffsetDateTime readAt);

    /** 방을 나갈 때 사용자 개인의 읽음 위치를 초기화한다. */
    void clearReadPosition(long roomId, long userId);

    record RoomSummary(ChatRoom room, int unreadCount, String lastMessage, OffsetDateTime lastMessageAt) {}
}
