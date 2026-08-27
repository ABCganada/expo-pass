package com.coderhan.lastmission.chat.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.coderhan.lastmission.chat.application.ChatRepository;
import com.coderhan.lastmission.chat.domain.ChatRoom;
import com.coderhan.lastmission.shared.realtime.StompUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSubscriptionGuardTest {
    private static final long USER_ID = 7L;
    private static final long ROOM_ID = 1194928000228884488L;
    private static final StompUser USER = new StompUser(USER_ID);

    @Mock ChatRepository chatRepository;
    @InjectMocks ChatSubscriptionGuard guard;

    @Test
    void claimsOnlyChatRoomDestinations() {
        assertThat(guard.supports("/topic/chat." + ROOM_ID)).isTrue();
        assertThat(guard.supports("/topic/notice")).isFalse();
        assertThat(guard.supports("/user/queue/notification")).isFalse();
    }

    @Test
    void allowsActivePublicRoomWithoutParticipation() {
        when(chatRepository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(true)));

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isTrue();
    }

    @Test
    void allowsPrivateRoomOnlyForCurrentParticipant() {
        when(chatRepository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(true)));
        when(chatRepository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(true);

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isTrue();
    }

    @Test
    void rejectsPrivateRoomForNonParticipant() {
        when(chatRepository.findRoom(ROOM_ID)).thenReturn(Optional.of(privateRoom(true)));
        when(chatRepository.isCurrentParticipant(ROOM_ID, USER_ID)).thenReturn(false);

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isFalse();
    }

    @Test
    void rejectsClosedRoom() {
        when(chatRepository.findRoom(ROOM_ID)).thenReturn(Optional.of(publicRoom(false)));

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isFalse();
    }

    @Test
    void rejectsUnknownRoom() {
        when(chatRepository.findRoom(ROOM_ID)).thenReturn(Optional.empty());

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isFalse();
    }

    @Test
    void rejectsWhenLookupFails() {
        when(chatRepository.findRoom(ROOM_ID))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("조회 지연"));

        assertThat(guard.canSubscribe(USER, "/topic/chat." + ROOM_ID)).isFalse();
    }

    @Test
    void rejectsMalformedRoomId() {
        assertThat(guard.canSubscribe(USER, "/topic/chat.abc")).isFalse();
        assertThat(guard.canSubscribe(USER, "/topic/chat.")).isFalse();
        assertThat(guard.canSubscribe(USER, "/topic/chat.-1")).isFalse();
    }

    private static ChatRoom publicRoom(boolean active) {
        return new ChatRoom(ROOM_ID, true, 1L, "공개 라운지",
                active, OffsetDateTime.parse("2026-07-01T00:00:00Z"));
    }

    private static ChatRoom privateRoom(boolean active) {
        return new ChatRoom(ROOM_ID, false, 1L, "비공개방",
                active, OffsetDateTime.parse("2026-07-01T00:00:00Z"));
    }
}
