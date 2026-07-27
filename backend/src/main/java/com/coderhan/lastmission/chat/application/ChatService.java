package com.coderhan.lastmission.chat.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.chat.domain.ChatRoom;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.shared.realtime.PresenceRegistry;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatRepository repository;
    private final ChatBroadcaster broadcaster;
    private final ChatDirectoryNotifier directoryNotifier;
    private final UserDirectory userDirectory;
    private final PresenceRegistry presenceRegistry;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RoomDirectory listRooms(long userId) {
        List<ChatRepository.RoomSummary> publicRooms = repository.findPublicRooms(userId);
        List<ChatRepository.RoomSummary> privateRooms = repository.findPrivateRooms(userId);
        int totalUnread = publicRooms.stream().mapToInt(ChatRepository.RoomSummary::unreadCount).sum()
                + privateRooms.stream().mapToInt(ChatRepository.RoomSummary::unreadCount).sum();
        return new RoomDirectory(publicRooms, privateRooms, totalUnread);
    }

    @Transactional
    public ChatRoom createRoom(
            long ownerUserId, String ownerName, boolean admin, String name, boolean isPublic) {
        if (isPublic && !admin) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "공개 채팅방은 관리자만 만들 수 있습니다.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        ChatRoom room = repository.createRoom(ownerUserId, ChatRoom.normalizeName(name), isPublic, now);
        repository.addParticipant(room.id(), ownerUserId, now);
        announceJoined(room.id(), ownerUserId, ownerName, now);
        if (isPublic) directoryNotifier.publicDirectoryChanged();
        else directoryNotifier.userChanged(ownerUserId);
        return room;
    }

    /** 관리자 접속자 목록에서 비공개방 생성과 상대 초대를 한 트랜잭션으로 처리한다. */
    @Transactional
    public ChatRoom startPrivateConversation(
            long ownerUserId, String ownerName, boolean admin, String email) {
        requireAdmin(admin);
        UserRef invited = userDirectory.findActiveByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_USER_NOT_FOUND,
                        "해당 이메일의 활성 사용자를 찾을 수 없습니다."));
        if (invited.id() == ownerUserId) {
            throw new BusinessException(ErrorCode.CHAT_PARTICIPANT_EXISTS, "본인은 초대할 수 없습니다.");
        }
        String suffix = "님과의 대화";
        String targetName = displayNameOf(invited);
        if (targetName.length() > ChatRoom.MAX_NAME_LENGTH - suffix.length()) {
            targetName = targetName.substring(0, ChatRoom.MAX_NAME_LENGTH - suffix.length());
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        ChatRoom room = repository.createRoom(
                ownerUserId, ChatRoom.normalizeName(targetName + suffix), false, now);
        repository.addParticipant(room.id(), ownerUserId, now);
        repository.addParticipant(room.id(), invited.id(), now);
        announceJoined(room.id(), ownerUserId, ownerName, now);
        announceJoined(room.id(), invited.id(), displayNameOf(invited), now);
        directoryNotifier.usersChanged(List.of(ownerUserId, invited.id()));
        return room;
    }

    /** 공개방을 실제로 열었을 때 현재 참가자로 기록한다. 여러 번 호출해도 한 행만 유지한다. */
    @Transactional
    public void joinPublicRoom(long roomId, long userId, String userName) {
        ChatRoom room = requireExistingActiveRoom(roomId);
        if (!room.isPublic()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID, "비공개 채팅방은 초대로만 참여할 수 있습니다.");
        }
        if (repository.isCurrentParticipant(roomId, userId)) return;
        OffsetDateTime now = OffsetDateTime.now(clock);
        try {
            repository.addParticipant(roomId, userId, now);
            announceJoined(roomId, userId, userName, now);
            directoryNotifier.usersChanged(affectedUsers(roomId));
        } catch (DuplicateKeyException ignored) {
            // 두 브라우저 탭이 동시에 열어도 활성 참가자는 하나만 있으면 된다.
        }
    }

    @Transactional
    public UserRef invite(long roomId, long inviterId, String email) {
        ChatRoom room = requireAccessibleRoom(roomId, inviterId);
        if (room.isPublic()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID, "공개 채팅방에는 초대가 필요하지 않습니다.");
        }
        UserRef invited = userDirectory.findActiveByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_USER_NOT_FOUND,
                        "해당 이메일의 활성 사용자를 찾을 수 없습니다."));
        if (repository.isCurrentParticipant(roomId, invited.id())) {
            throw new BusinessException(ErrorCode.CHAT_PARTICIPANT_EXISTS, "이미 참여 중인 사용자입니다.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        try {
            repository.addParticipant(roomId, invited.id(), now);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CHAT_PARTICIPANT_EXISTS,
                    "이미 참여 중인 사용자입니다.");
        }
        announceJoined(roomId, invited.id(), displayNameOf(invited), now);
        directoryNotifier.usersChanged(affectedUsers(roomId));
        return invited;
    }

    @Transactional(readOnly = true)
    public List<RoomParticipant> participants(long roomId, long currentUserId) {
        requireAccessibleRoom(roomId, currentUserId);
        List<Long> participantIds = repository.findCurrentParticipantIds(roomId);
        return userDirectory.findActiveByIds(participantIds).stream()
                .map(user -> new RoomParticipant(
                        user, user.id() == currentUserId, presenceRegistry.isOnline(user.id())))
                .sorted(Comparator
                        .comparingInt((RoomParticipant participant) ->
                                participant.self() ? 0 : participant.online() ? 1 : 2)
                        .thenComparing(participant -> displayNameOf(participant.user()).toLowerCase(Locale.ROOT))
                        .thenComparingLong(participant -> participant.user().id()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectoryUser> searchUsers(long currentUserId, boolean admin, String query, Integer limit) {
        requireAdmin(admin);
        int size = limit == null ? 30 : Math.clamp(limit, 1, 100);
        return userDirectory.searchActive(query, size).stream()
                .filter(user -> user.id() != currentUserId)
                .map(this::directoryUser)
                .sorted(directoryUserComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectoryUser> onlineUsers(long currentUserId, boolean admin) {
        requireAdmin(admin);
        return userDirectory.findActiveByIds(presenceRegistry.onlineUserIds()).stream()
                .map(this::directoryUser)
                .sorted(Comparator
                        .comparingInt((DirectoryUser user) -> user.user().id() == currentUserId ? 0 : 1)
                        .thenComparing(user -> displayNameOf(user.user()).toLowerCase(Locale.ROOT))
                        .thenComparingLong(user -> user.user().id()))
                .toList();
    }

    @Transactional
    public void leave(long roomId, long userId, String userName) {
        ChatRoom room = requireAccessibleRoom(roomId, userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!repository.leaveParticipant(roomId, userId, now)) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "현재 참여 중인 채팅방이 아닙니다.");
        }
        if (room.isPublic()) {
            repository.clearReadPosition(roomId, userId);
        }
        ChatMessage leaveEvent = repository.saveSystemEvent(
                roomId, userId, ChatMessageType.MEMBER_LEFT,
                displayNameOf(userName) + "님이 퇴장하였습니다.", now);
        broadcaster.broadcast(leaveEvent);
        directoryNotifier.usersChanged(affectedUsers(roomId, userId));
    }

    /**
     * 방의 메시지를 오래된 순으로 돌려준다. 접근 권한이 없으면 거절한다.
     *
     * <p>조회는 상태를 바꾸지 않는다. 읽음 처리는 {@link #markRead} 를 따로 호출한다.</p>
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> findMessages(long roomId, long userId, Long beforeId, Integer limit) {
        ChatRoom room = requireAccessibleRoom(roomId, userId);
        int size = limit == null ? DEFAULT_PAGE_SIZE : Math.clamp(limit, 1, MAX_PAGE_SIZE);
        List<ChatMessage> latestFirst = repository.findMessages(
                roomId, visibleAfter(room, userId), beforeId, size);

        List<ChatMessage> oldestFirst = new ArrayList<>(latestFirst);
        java.util.Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    @Transactional
    public ChatMessage send(long roomId, long senderId, String senderName, String content) {
        ChatRoom room = requireAccessibleRoom(roomId, senderId);
        if (room.isPublic()) {
            requireCurrentParticipant(roomId, senderId);
        }
        if (!room.active()) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "닫힌 채팅방입니다.");
        }
        ChatMessage saved = repository.save(roomId, senderId, displayNameOf(senderName),
                ChatMessage.normalizeContent(content), OffsetDateTime.now(clock));
        broadcaster.broadcast(saved);
        directoryNotifier.usersChanged(affectedUsers(roomId));
        return saved;
    }

    /** 이 방에서 사용자가 안 읽은 메시지 수. */
    @Transactional(readOnly = true)
    public int unreadCount(long roomId, long userId) {
        ChatRoom room = requireAccessibleRoom(roomId, userId);
        if (room.isPublic() && !repository.isCurrentParticipant(roomId, userId)) {
            return 0;
        }
        return repository.countUnread(roomId, userId, visibleAfter(room, userId), room.isPublic());
    }

    /** 방을 최신 메시지까지 읽은 것으로 표시한다. */
    @Transactional
    public void markRead(long roomId, long userId) {
        ChatRoom room = requireAccessibleRoom(roomId, userId);
        if (room.isPublic()) {
            requireCurrentParticipant(roomId, userId);
        }
        repository.markRead(roomId, userId, repository.findLatestMessageId(roomId), OffsetDateTime.now(clock));
        directoryNotifier.userChanged(userId);
    }

    private ChatRoom requireExistingActiveRoom(long roomId) {
        ChatRoom room = repository.findRoom(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (!room.active()) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "닫힌 채팅방입니다.");
        }
        return room;
    }

    private ChatRoom requireAccessibleRoom(long roomId, long userId) {
        ChatRoom room = requireExistingActiveRoom(roomId);
        if (!room.isPublic() && !repository.isCurrentParticipant(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "접근할 수 없는 채팅방입니다.");
        }
        return room;
    }

    private void requireCurrentParticipant(long roomId, long userId) {
        if (!repository.isCurrentParticipant(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED,
                    "채팅방에 먼저 참여해야 합니다.");
        }
    }

    private void announceJoined(
            long roomId, long userId, String userName, OffsetDateTime joinedAt) {
        ChatMessage joinEvent = repository.saveSystemEvent(
                roomId, userId, ChatMessageType.MEMBER_JOINED,
                displayNameOf(userName) + "님이 입장하였습니다.", joinedAt);
        broadcaster.broadcast(joinEvent);
    }

    private List<Long> affectedUsers(long roomId, long... additionalUserIds) {
        List<Long> userIds = new ArrayList<>(repository.findCurrentParticipantIds(roomId));
        for (long userId : additionalUserIds) {
            if (userId > 0 && !userIds.contains(userId)) userIds.add(userId);
        }
        return userIds;
    }

    private OffsetDateTime visibleAfter(ChatRoom room, long userId) {
        return repository.findCurrentJoinedAt(room.id(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ACCESS_DENIED,
                        "현재 참여 중인 채팅방이 아닙니다."));
    }

    private DirectoryUser directoryUser(UserRef user) {
        return new DirectoryUser(user, presenceRegistry.isOnline(user.id()),
                presenceRegistry.connectedAt(user.id()).orElse(null));
    }

    private Comparator<DirectoryUser> directoryUserComparator() {
        return Comparator
                .comparing(DirectoryUser::online).reversed()
                .thenComparing(user -> displayNameOf(user.user()).toLowerCase(Locale.ROOT))
                .thenComparingLong(user -> user.user().id());
    }

    private void requireAdmin(boolean admin) {
        if (!admin) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED, "관리자만 사용자 목록을 조회할 수 있습니다.");
        }
    }

    private String displayNameOf(UserRef user) {
        if (user.name() != null && !user.name().isBlank()) return user.name().trim();
        return user.email();
    }

    /** 표시 이름이 비어 있으면 익명으로 남긴다. 이름 없음 때문에 발신이 막히지는 않게 한다. */
    private String displayNameOf(String senderName) {
        return senderName == null || senderName.isBlank() ? "익명" : senderName.trim();
    }

    public record RoomDirectory(
            List<ChatRepository.RoomSummary> publicRooms,
            List<ChatRepository.RoomSummary> privateRooms,
            int totalUnread
    ) {}

    public record RoomParticipant(UserRef user, boolean self, boolean online) {}

    public record DirectoryUser(UserRef user, boolean online, OffsetDateTime connectedAt) {}
}
