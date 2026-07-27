import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { chatService } from "../services/chatService";
import type {
  ChatDirectoryUser,
  ChatMessage,
  ChatParticipant,
  ChatRoom,
  ChatRoomDirectory,
  InvitedUser,
} from "../types/chat";

export interface ChatMessageQuery {
  roomId: string;
  before?: string;
}

interface CreateRoomCommand {
  name: string;
  isPublic: boolean;
}

interface EmailCommand {
  roomId: string;
  email: string;
}

interface SendMessageCommand {
  roomId: string;
  content: string;
}

export function mergeChatMessages(
  current: ChatMessage[],
  incoming: ChatMessage[],
): ChatMessage[] {
  const merged = new Map<string, ChatMessage>();
  for (const message of current) merged.set(message.id, message);
  for (const message of incoming) merged.set(message.id, message);
  return Array.from(merged.values())
    .sort((left, right) => Number(left.id) - Number(right.id));
}

export const chatApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getChatRooms: build.query<ChatRoomDirectory, void>({
      queryFn: (_argument, api) =>
        queryResult(chatService.findRooms(api.signal)),
      providesTags: [{ type: "ChatRoom", id: "LIST" }],
      keepUnusedDataFor: 300,
    }),
    getOnlineUsers: build.query<ChatDirectoryUser[], void>({
      queryFn: (_argument, api) =>
        queryResult(chatService.findOnlineUsers(api.signal)),
      providesTags: [{ type: "ChatUser", id: "ONLINE" }],
    }),
    searchChatUsers: build.query<ChatDirectoryUser[], string>({
      queryFn: (query, api) =>
        queryResult(chatService.searchUsers(query, api.signal)),
      providesTags: [{ type: "ChatUser", id: "SEARCH" }],
    }),
    getChatParticipants: build.query<ChatParticipant[], string>({
      queryFn: (roomId, api) =>
        queryResult(chatService.findParticipants(roomId, api.signal)),
      providesTags: (_result, _error, roomId) => [
        { type: "ChatParticipant", id: roomId },
      ],
    }),
    getChatMessages: build.query<ChatMessage[], ChatMessageQuery>({
      queryFn: ({ roomId, before }, api) =>
        queryResult(chatService.findMessages(roomId, before, api.signal)),
      serializeQueryArgs: ({ endpointName, queryArgs }) =>
        `${endpointName}:${queryArgs.roomId}`,
      merge: (currentCache, incoming) =>
        mergeChatMessages(currentCache, incoming),
      forceRefetch: ({ currentArg, previousArg }) =>
        currentArg?.before !== previousArg?.before,
      providesTags: (_result, _error, { roomId }) => [
        { type: "ChatMessage", id: roomId },
      ],
    }),
    createChatRoom: build.mutation<ChatRoom, CreateRoomCommand>({
      queryFn: ({ name, isPublic }) =>
        queryResult(chatService.createRoom(name, isPublic)),
      invalidatesTags: [{ type: "ChatRoom", id: "LIST" }],
    }),
    startConversation: build.mutation<ChatRoom, string>({
      queryFn: (email) => queryResult(chatService.startConversation(email)),
      invalidatesTags: [{ type: "ChatRoom", id: "LIST" }],
    }),
    joinChatRoom: build.mutation<void, string>({
      queryFn: (roomId) => queryResult(chatService.join(roomId)),
      invalidatesTags: (_result, _error, roomId) => [
        { type: "ChatRoom", id: "LIST" },
        { type: "ChatParticipant", id: roomId },
        { type: "ChatMessage", id: roomId },
      ],
    }),
    inviteChatParticipant: build.mutation<InvitedUser, EmailCommand>({
      queryFn: ({ roomId, email }) =>
        queryResult(chatService.invite(roomId, email)),
      invalidatesTags: (_result, _error, { roomId }) => [
        { type: "ChatRoom", id: "LIST" },
        { type: "ChatParticipant", id: roomId },
      ],
    }),
    leaveChatRoom: build.mutation<void, string>({
      queryFn: (roomId) => queryResult(chatService.leave(roomId)),
      invalidatesTags: (_result, _error, roomId) => [
        { type: "ChatRoom", id: "LIST" },
        { type: "ChatParticipant", id: roomId },
        { type: "ChatMessage", id: roomId },
      ],
    }),
    sendChatMessage: build.mutation<ChatMessage, SendMessageCommand>({
      queryFn: ({ roomId, content }) =>
        queryResult(chatService.send(roomId, content)),
      invalidatesTags: [{ type: "ChatRoom", id: "LIST" }],
    }),
    markChatRoomRead: build.mutation<void, string>({
      queryFn: (roomId) => queryResult(chatService.markRead(roomId)),
      invalidatesTags: [{ type: "ChatRoom", id: "LIST" }],
    }),
  }),
});

export const {
  useGetChatRoomsQuery,
  useGetOnlineUsersQuery,
  useSearchChatUsersQuery,
  useGetChatParticipantsQuery,
  useGetChatMessagesQuery,
  useLazyGetChatMessagesQuery,
  useCreateChatRoomMutation,
  useStartConversationMutation,
  useJoinChatRoomMutation,
  useInviteChatParticipantMutation,
  useLeaveChatRoomMutation,
  useSendChatMessageMutation,
  useMarkChatRoomReadMutation,
} = chatApi;
