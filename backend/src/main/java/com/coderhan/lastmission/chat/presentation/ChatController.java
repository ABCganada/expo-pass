package com.coderhan.lastmission.chat.presentation;

import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.chat.application.ChatRepository;
import com.coderhan.lastmission.chat.application.ChatService;
import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.chat.domain.ChatRoom;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅 API.
 *
 * <p>ID 는 문자열로 내보낸다. CockroachDB 의 id 가 2^53 을 넘어 JS Number 로는 정확히
 * 표현되지 않기 때문이다(프로젝트 공통 규약).</p>
 *
 * <p>공개방은 전원, 비공개방은 현재 참가자만 접근한다. 관리자도 비공개방 특례가 없다.</p>
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
class ChatController {
    private final ChatService chatService;

    @GetMapping("/rooms")
    ApiResponse<RoomDirectoryResponse> rooms(@AuthenticationPrincipal LastMissionPrincipal principal) {
        return ApiResponse.success(RoomDirectoryResponse.from(chatService.listRooms(principal.userId())));
    }

    @PostMapping("/rooms")
    ResponseEntity<ApiResponse<RoomResponse>> createRoom(@RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal,
            Authentication authentication) {
        ChatRoom room = chatService.createRoom(
                principal.userId(), displayName(principal), isAdmin(authentication),
                request == null ? null : request.name(), request != null && request.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(RoomResponse.from(room)));
    }

    @PostMapping("/conversations")
    ResponseEntity<ApiResponse<RoomResponse>> startConversation(
            @RequestBody ConversationRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal,
            Authentication authentication) {
        ChatRoom room = chatService.startPrivateConversation(
                principal.userId(), displayName(principal), isAdmin(authentication),
                request == null ? null : request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("비공개 채팅방을 만들었습니다.", RoomResponse.from(room)));
    }

    @PostMapping("/rooms/{roomId}/join")
    ApiResponse<Void> join(@PathVariable String roomId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        chatService.joinPublicRoom(
                parseId(roomId, "채팅방"), principal.userId(), displayName(principal));
        return ApiResponse.success("공개 채팅방에 참여했습니다.", null);
    }

    @GetMapping("/rooms/{roomId}/participants")
    ApiResponse<List<ParticipantResponse>> participants(@PathVariable String roomId,
            @AuthenticationPrincipal LastMissionPrincipal principal,
            Authentication authentication) {
        boolean admin = isAdmin(authentication);
        List<ParticipantResponse> participants = chatService
                .participants(parseId(roomId, "채팅방"), principal.userId())
                .stream()
                .map(participant -> ParticipantResponse.from(participant, admin))
                .toList();
        return ApiResponse.success(participants);
    }

    @PostMapping("/rooms/{roomId}/participants")
    ResponseEntity<ApiResponse<InvitedUserResponse>> invite(@PathVariable String roomId,
            @RequestBody InviteRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        UserRef invited = chatService.invite(parseId(roomId, "채팅방"), principal.userId(),
                request == null ? null : request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사용자를 초대했습니다.", InvitedUserResponse.from(invited)));
    }

    @PostMapping("/rooms/{roomId}/leave")
    ApiResponse<Void> leave(@PathVariable String roomId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        chatService.leave(
                parseId(roomId, "채팅방"), principal.userId(), displayName(principal));
        return ApiResponse.success("채팅방에서 퇴장하였습니다.", null);
    }

    @GetMapping("/users")
    ApiResponse<List<DirectoryUserResponse>> users(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "30") Integer limit,
            @AuthenticationPrincipal LastMissionPrincipal principal,
            Authentication authentication) {
        return ApiResponse.success(chatService
                .searchUsers(principal.userId(), isAdmin(authentication), query, limit)
                .stream().map(user -> DirectoryUserResponse.from(user, principal.userId()))
                .toList());
    }

    @GetMapping("/online-users")
    ApiResponse<List<DirectoryUserResponse>> onlineUsers(
            @AuthenticationPrincipal LastMissionPrincipal principal,
            Authentication authentication) {
        return ApiResponse.success(chatService
                .onlineUsers(principal.userId(), isAdmin(authentication))
                .stream().map(user -> DirectoryUserResponse.from(user, principal.userId()))
                .toList());
    }

    @GetMapping("/rooms/{roomId}/messages")
    ApiResponse<List<MessageResponse>> findMessages(@PathVariable String roomId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        List<MessageResponse> messages = chatService
                .findMessages(parseId(roomId, "채팅방"), principal.userId(), parseCursor(before), limit)
                .stream().map(MessageResponse::from).toList();
        return ApiResponse.success(messages);
    }

    @PostMapping("/rooms/{roomId}/messages")
    ResponseEntity<ApiResponse<MessageResponse>> send(@PathVariable String roomId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        ChatMessage saved = chatService.send(parseId(roomId, "채팅방"), principal.userId(),
                principal.name(), request == null ? null : request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageResponse.from(saved)));
    }

    @GetMapping("/rooms/{roomId}/unread")
    ApiResponse<UnreadResponse> unread(@PathVariable String roomId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        int count = chatService.unreadCount(parseId(roomId, "채팅방"), principal.userId());
        return ApiResponse.success(new UnreadResponse(count));
    }

    @PostMapping("/rooms/{roomId}/read")
    ApiResponse<Void> markRead(@PathVariable String roomId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        chatService.markRead(parseId(roomId, "채팅방"), principal.userId());
        return ApiResponse.success("읽음 처리되었습니다.", null);
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private static String displayName(LastMissionPrincipal principal) {
        return principal.name() == null || principal.name().isBlank()
                ? principal.email()
                : principal.name();
    }

    private long parseId(String value, String label) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND, label + "을 찾을 수 없습니다.");
        }
    }

    private Long parseCursor(String before) {
        if (before == null || before.isBlank()) return null;
        try {
            return Long.parseLong(before);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND, "잘못된 조회 위치입니다.");
        }
    }

    record SendMessageRequest(String content) {}

    record CreateRoomRequest(String name, boolean isPublic) {}

    record ConversationRequest(String email) {}

    record InviteRequest(String email) {}

    record UnreadResponse(int unreadCount) {}

    record RoomResponse(String id, boolean isPublic, String ownerUserId, String name) {
        static RoomResponse from(ChatRoom room) {
            return new RoomResponse(Long.toString(room.id()), room.isPublic(),
                    Long.toString(room.ownerUserId()), room.name());
        }
    }

    record RoomSummaryResponse(String id, boolean isPublic, String ownerUserId, String name,
            int unreadCount, String lastMessage, OffsetDateTime lastMessageAt) {
        static RoomSummaryResponse from(ChatRepository.RoomSummary summary) {
            ChatRoom room = summary.room();
            return new RoomSummaryResponse(Long.toString(room.id()), room.isPublic(),
                    Long.toString(room.ownerUserId()), room.name(), summary.unreadCount(),
                    summary.lastMessage(), summary.lastMessageAt());
        }
    }

    record RoomDirectoryResponse(
            List<RoomSummaryResponse> publicRooms,
            List<RoomSummaryResponse> privateRooms,
            int totalUnread
    ) {
        static RoomDirectoryResponse from(ChatService.RoomDirectory directory) {
            return new RoomDirectoryResponse(
                    directory.publicRooms().stream().map(RoomSummaryResponse::from).toList(),
                    directory.privateRooms().stream().map(RoomSummaryResponse::from).toList(),
                    directory.totalUnread());
        }
    }

    record ParticipantResponse(
            String id, String name, String email, boolean self, boolean online
    ) {
        static ParticipantResponse from(ChatService.RoomParticipant participant, boolean admin) {
            UserRef user = participant.user();
            String displayName = user.name() == null || user.name().isBlank() ? user.email() : user.name();
            return new ParticipantResponse(Long.toString(user.id()), displayName,
                    admin ? user.email() : null, participant.self(), participant.online());
        }
    }

    record DirectoryUserResponse(
            String id, String name, String email, boolean online, boolean self,
            OffsetDateTime connectedAt
    ) {
        static DirectoryUserResponse from(ChatService.DirectoryUser directoryUser, long currentUserId) {
            UserRef user = directoryUser.user();
            String displayName = user.name() == null || user.name().isBlank() ? user.email() : user.name();
            return new DirectoryUserResponse(Long.toString(user.id()), displayName, user.email(),
                    directoryUser.online(), user.id() == currentUserId, directoryUser.connectedAt());
        }
    }

    record InvitedUserResponse(String id, String email, String name) {
        static InvitedUserResponse from(UserRef user) {
            String displayName = user.name() == null || user.name().isBlank()
                    ? user.email() : user.name();
            return new InvitedUserResponse(Long.toString(user.id()), user.email(), displayName);
        }
    }

    record MessageResponse(String id, String roomId, String senderId, String senderName,
            String content, ChatMessageType messageType, boolean deleted, OffsetDateTime createdAt) {
        static MessageResponse from(ChatMessage message) {
            return new MessageResponse(Long.toString(message.id()), Long.toString(message.roomId()),
                    Long.toString(message.senderId()), message.senderName(), message.displayContent(),
                    message.messageType(), message.isDeleted(), message.createdAt());
        }
    }
}
