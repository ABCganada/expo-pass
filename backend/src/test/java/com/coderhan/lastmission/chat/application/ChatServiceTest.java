package com.coderhan.lastmission.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.chat.domain.ChatRoom;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.shared.realtime.PresenceRegistry;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    private static final long ROOM_ID = 5L;
    private static final long USER_ID = 7L;
    private static final long OTHER_ID = 99L;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock ChatRepository repository;
    @Mock ChatBroadcaster broadcaster;
    @Mock ChatDirectoryNotifier directoryNotifier;
    @Mock UserDirectory userDirectory;
    @Mock PresenceRegistry presenceRegistry;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks ChatService service;

    @Test
    void listsPublicAndParticipatingPrivateRoomsWithTotalUnread() {
        ChatRepository.RoomSummary publicSummary =
                new ChatRepository.RoomSummary(publicRoom(true), 2, "공개 메시지", NOW);
        ChatRepository.RoomSummary privateSummary =
                new ChatRepository.RoomSummary(privateRoom(USER_ID, true), 3, "비공개 메시지", NOW);
        when(repository.findPublicRooms(USER_ID)).thenReturn(List.of(publicSummary));
        when(repository.findPrivateRooms(USER_ID)).thenReturn(List.of(privateSummary));

        ChatService.RoomDirectory directory = service.listRooms(USER_ID);

        assertThat(directory.publicRooms()).containsExactly(publicSummary);
        assertThat(directory.privateRooms()).containsExactly(privateSummary);
        assertThat(directory.totalUnread()).isEqualTo(5);
    }

    @Test
    void createsPrivateRoomAndJoinsOwner() {
        ChatRoom created = privateRoom(USER_ID, true);
        ChatMessage joinEvent = systemMessage(
                USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.");
        when(repository.createRoom(USER_ID, "스터디방", false, NOW)).thenReturn(created);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.", NOW))
                .thenReturn(joinEvent);

        ChatRoom room = service.createRoom(USER_ID, "한대천", false, "  스터디방  ", false);

        assertThat(room).isSameAs(created);
        verify(repository).addParticipant(ROOM_ID, USER_ID, NOW);
        verify(broadcaster).broadcast(joinEvent);
    }

    @Test
    void rejectsPublicRoomCreationByRegularUser() {
        assertThatThrownBy(() -> service.createRoom(USER_ID, "한대천", false, "공개방", true))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED));

        verify(repository, never()).createRoom(anyLong(), any(), any(Boolean.class), any());
    }

    @Test
    void allowsAdminToCreatePublicRoomAndJoinsOwner() {
        ChatRoom created = publicRoom(true);
        ChatMessage joinEvent = systemMessage(
                USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.");
        when(repository.createRoom(USER_ID, "공개 라운지", true, NOW)).thenReturn(created);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.", NOW))
                .thenReturn(joinEvent);

        service.createRoom(USER_ID, "한대천", true, "공개 라운지", true);

        verify(repository).createRoom(USER_ID, "공개 라운지", true, NOW);
        verify(repository).addParticipant(ROOM_ID, USER_ID, NOW);
        verify(broadcaster).broadcast(joinEvent);
    }

    @Test
    void startsAdminPrivateConversationInOneTransaction() {
        UserRef invited = new UserRef(OTHER_ID, "guest@example.com", "게스트");
        ChatRoom created = privateRoom(USER_ID, true);
        ChatMessage ownerJoin = systemMessage(
                USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.");
        ChatMessage invitedJoin = systemMessage(
                OTHER_ID, ChatMessageType.MEMBER_JOINED, "게스트님이 입장하였습니다.");
        when(userDirectory.findActiveByEmail("guest@example.com")).thenReturn(Optional.of(invited));
        when(repository.createRoom(USER_ID, "게스트님과의 대화", false, NOW)).thenReturn(created);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.", NOW))
                .thenReturn(ownerJoin);
        when(repository.saveSystemEvent(
                ROOM_ID, OTHER_ID, ChatMessageType.MEMBER_JOINED, "게스트님이 입장하였습니다.", NOW))
                .thenReturn(invitedJoin);

        ChatRoom room = service.startPrivateConversation(
                USER_ID, "한대천", true, "guest@example.com");

        assertThat(room).isSameAs(created);
        verify(repository).addParticipant(ROOM_ID, USER_ID, NOW);
        verify(repository).addParticipant(ROOM_ID, OTHER_ID, NOW);
        verify(broadcaster).broadcast(ownerJoin);
        verify(broadcaster).broadcast(invitedJoin);
    }

    @Test
    void joinsPublicRoomWhenItIsActuallyOpened() {
        ChatMessage joinEvent = systemMessage(
                USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(false);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_JOINED, "한대천님이 입장하였습니다.", NOW))
                .thenReturn(joinEvent);

        service.joinPublicRoom(ROOM_ID, USER_ID, "한대천");

        verify(repository).addParticipant(ROOM_ID, USER_ID, NOW);
        verify(broadcaster).broadcast(joinEvent);
    }

    @Test
    void ordersParticipantsAsSelfThenOnlineThenOffline() {
        UserRef self = new UserRef(USER_ID, "me@example.com", "나");
        UserRef online = new UserRef(OTHER_ID, "online@example.com", "온라인");
        UserRef offline = new UserRef(101L, "offline@example.com", "오프라인");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.findCurrentParticipantIds(ROOM_ID)).thenReturn(List.of(OTHER_ID, 101L, USER_ID));
        when(userDirectory.findActiveByIds(List.of(OTHER_ID, 101L, USER_ID)))
                .thenReturn(List.of(offline, online, self));
        when(presenceRegistry.isOnline(anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0, Long.class) == OTHER_ID);

        List<ChatService.RoomParticipant> participants = service.participants(ROOM_ID, USER_ID);

        assertThat(participants).extracting(participant -> participant.user().id())
                .containsExactly(USER_ID, OTHER_ID, 101L);
    }

    @Test
    void includesCurrentUserFirstInAdminOnlineUsers() {
        UserRef self = new UserRef(USER_ID, "me@example.com", "나");
        UserRef other = new UserRef(OTHER_ID, "other@example.com", "다른 사용자");
        java.util.Set<Long> onlineIds = java.util.Set.of(USER_ID, OTHER_ID);
        when(presenceRegistry.onlineUserIds()).thenReturn(onlineIds);
        when(userDirectory.findActiveByIds(onlineIds)).thenReturn(List.of(other, self));
        when(presenceRegistry.isOnline(anyLong())).thenReturn(true);

        List<ChatService.DirectoryUser> users = service.onlineUsers(USER_ID, true);

        assertThat(users).extracting(user -> user.user().id())
                .containsExactly(USER_ID, OTHER_ID);
    }

    @Test
    void invitesActiveUserByExactEmail() {
        UserRef invited = new UserRef(OTHER_ID, "guest@example.com", "게스트");
        ChatMessage joinEvent = systemMessage(
                OTHER_ID, ChatMessageType.MEMBER_JOINED, "게스트님이 입장하였습니다.");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(userDirectory.findActiveByEmail("guest@example.com")).thenReturn(Optional.of(invited));
        when(repository.isCurrentParticipant(ROOM_ID, OTHER_ID)).thenReturn(false);
        when(repository.saveSystemEvent(
                ROOM_ID, OTHER_ID, ChatMessageType.MEMBER_JOINED, "게스트님이 입장하였습니다.", NOW))
                .thenReturn(joinEvent);

        UserRef result = service.invite(ROOM_ID, USER_ID, "guest@example.com");

        assertThat(result).isSameAs(invited);
        verify(repository).addParticipant(ROOM_ID, OTHER_ID, NOW);
        verify(broadcaster).broadcast(joinEvent);
    }

    @Test
    void rejectsInvitationWhenUserAlreadyParticipates() {
        UserRef invited = new UserRef(OTHER_ID, "guest@example.com", "게스트");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(userDirectory.findActiveByEmail("guest@example.com")).thenReturn(Optional.of(invited));
        when(repository.isCurrentParticipant(ROOM_ID, OTHER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.invite(ROOM_ID, USER_ID, "guest@example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_PARTICIPANT_EXISTS));

        verify(repository, never()).addParticipant(anyLong(), anyLong(), any());
    }

    @Test
    void mapsConcurrentDuplicateInvitationToBusinessError() {
        UserRef invited = new UserRef(OTHER_ID, "guest@example.com", "게스트");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(userDirectory.findActiveByEmail("guest@example.com")).thenReturn(Optional.of(invited));
        when(repository.isCurrentParticipant(ROOM_ID, OTHER_ID)).thenReturn(false);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("동시 초대"))
                .when(repository).addParticipant(ROOM_ID, OTHER_ID, NOW);

        assertThatThrownBy(() -> service.invite(ROOM_ID, USER_ID, "guest@example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_PARTICIPANT_EXISTS));
    }

    @Test
    void rejectsInvitationToPublicRoom() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));

        assertThatThrownBy(() -> service.invite(ROOM_ID, USER_ID, "guest@example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_ROOM_INVALID));
    }

    @Test
    void leavesParticipatingPrivateRoom() {
        ChatMessage leaveEvent = systemMessage(
                USER_ID, ChatMessageType.MEMBER_LEFT, "한대천님이 퇴장하였습니다.");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.leaveParticipant(ROOM_ID, USER_ID, NOW)).thenReturn(true);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_LEFT, "한대천님이 퇴장하였습니다.", NOW))
                .thenReturn(leaveEvent);

        service.leave(ROOM_ID, USER_ID, "한대천");

        verify(repository).leaveParticipant(ROOM_ID, USER_ID, NOW);
        verify(broadcaster).broadcast(leaveEvent);
    }

    @Test
    void leavesPublicRoomParticipantRoster() {
        ChatMessage leaveEvent = systemMessage(
                USER_ID, ChatMessageType.MEMBER_LEFT, "한대천님이 퇴장하였습니다.");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.leaveParticipant(ROOM_ID, USER_ID, NOW)).thenReturn(true);
        when(repository.saveSystemEvent(
                ROOM_ID, USER_ID, ChatMessageType.MEMBER_LEFT, "한대천님이 퇴장하였습니다.", NOW))
                .thenReturn(leaveEvent);

        service.leave(ROOM_ID, USER_ID, "한대천");

        verify(repository).leaveParticipant(ROOM_ID, USER_ID, NOW);
        verify(repository).clearReadPosition(ROOM_ID, USER_ID);
        verify(broadcaster).broadcast(leaveEvent);
    }

    @Test
    void returnsMessagesOldestFirstAndOnlyAfterPrivateJoin() {
        OffsetDateTime joinedAt = NOW.minusHours(1);
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.findCurrentJoinedAt(ROOM_ID, USER_ID)).thenReturn(Optional.of(joinedAt));
        when(repository.findMessages(ROOM_ID, joinedAt, null, 50))
                .thenReturn(List.of(message(30L, "세 번째"), message(20L, "두 번째"), message(10L, "첫 번째")));

        List<ChatMessage> messages = service.findMessages(ROOM_ID, USER_ID, null, null);

        assertThat(messages).extracting(ChatMessage::content)
                .containsExactly("첫 번째", "두 번째", "세 번째");
    }

    @Test
    void publicMessageLookupStartsAtCurrentJoinTime() {
        OffsetDateTime joinedAt = NOW.minusMinutes(5);
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.findCurrentJoinedAt(ROOM_ID, USER_ID)).thenReturn(Optional.of(joinedAt));
        when(repository.findMessages(ROOM_ID, joinedAt, null, 100)).thenReturn(List.of());

        service.findMessages(ROOM_ID, USER_ID, null, 5000);

        verify(repository).findMessages(ROOM_ID, joinedAt, null, 100);
    }

    @Test
    void rejectsPrivateRoomForNonParticipantEvenWhenCallerCouldBeAdmin() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(OTHER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.send(ROOM_ID, USER_ID, "관리자", "안녕"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED));

        verify(broadcaster, never()).broadcast(any());
    }

    @Test
    void trimsAndBroadcastsSavedMessage() {
        ChatMessage saved = message(11L, "안녕하세요");
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.save(ROOM_ID, USER_ID, "한대천", "안녕하세요", NOW)).thenReturn(saved);

        ChatMessage result = service.send(ROOM_ID, USER_ID, " 한대천 ", "  안녕하세요  ");

        assertThat(result).isSameAs(saved);
        verify(broadcaster).broadcast(saved);
    }

    @Test
    void fallsBackToAnonymousWhenNameIsMissing() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.save(ROOM_ID, USER_ID, "익명", "안녕", NOW)).thenReturn(message(1L, "안녕"));

        service.send(ROOM_ID, USER_ID, "  ", "안녕");

        verify(repository).save(ROOM_ID, USER_ID, "익명", "안녕", NOW);
    }

    @Test
    void publicUnreadRequiresExistingReadMarker() {
        OffsetDateTime joinedAt = NOW.minusMinutes(5);
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.findCurrentJoinedAt(ROOM_ID, USER_ID)).thenReturn(Optional.of(joinedAt));
        when(repository.countUnread(ROOM_ID, USER_ID, joinedAt, true)).thenReturn(4);

        assertThat(service.unreadCount(ROOM_ID, USER_ID)).isEqualTo(4);
    }

    @Test
    void publicUnreadIsZeroAfterLeaving() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(false);

        assertThat(service.unreadCount(ROOM_ID, USER_ID)).isZero();

        verify(repository, never()).countUnread(anyLong(), anyLong(), any(), any(Boolean.class));
    }

    @Test
    void privateUnreadStartsAtCurrentJoinTime() {
        OffsetDateTime joinedAt = NOW.minusDays(1);
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(USER_ID, true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.findCurrentJoinedAt(ROOM_ID, USER_ID)).thenReturn(Optional.of(joinedAt));
        when(repository.countUnread(ROOM_ID, USER_ID, joinedAt, false)).thenReturn(2);

        assertThat(service.unreadCount(ROOM_ID, USER_ID)).isEqualTo(2);
    }

    @Test
    void marksRoomReadUpToLatestMessage() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));
        when(repository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);
        when(repository.findLatestMessageId(ROOM_ID)).thenReturn(42L);

        service.markRead(ROOM_ID, USER_ID);

        verify(repository).markRead(ROOM_ID, USER_ID, 42L, NOW);
    }

    @Test
    void rejectsUnknownRoom() {
        when(repository.findRoom(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(ROOM_ID, USER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Test
    void hidesContentOfDeletedMessages() {
        ChatMessage deleted = new ChatMessage(
                1L, ROOM_ID, USER_ID, "한대천", "원본", ChatMessageType.USER, NOW, NOW);

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.displayContent()).isEqualTo("삭제된 메시지입니다.");
    }

    private ChatRoom publicRoom(boolean active) {
        return new ChatRoom(ROOM_ID, true, USER_ID, "공개 라운지", active, NOW);
    }

    private ChatRoom privateRoom(long ownerId, boolean active) {
        return new ChatRoom(ROOM_ID, false, ownerId, "비공개방", active, NOW);
    }

    private ChatMessage message(long id, String content) {
        return new ChatMessage(
                id, ROOM_ID, USER_ID, "한대천", content, ChatMessageType.USER, NOW, null);
    }

    private ChatMessage systemMessage(long actorUserId, ChatMessageType messageType, String content) {
        return new ChatMessage(
                40L, ROOM_ID, actorUserId, "시스템", content, messageType, NOW, null);
    }
}
