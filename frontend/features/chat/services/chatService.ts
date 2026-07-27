import { getCsrfToken } from "../../shared/api/csrf";
import type {
  ChatDirectoryUser,
  ChatMessage,
  ChatMessageType,
  ChatParticipant,
  ChatRoom,
  ChatRoomDirectory,
  ChatRoomSummary,
  InvitedUser,
} from "../types/chat";
import { CHAT_MESSAGE_TYPES } from "../types/chat";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isChatMessageType(value: unknown): value is ChatMessageType {
  return typeof value === "string" &&
    CHAT_MESSAGE_TYPES.some((messageType) => messageType === value);
}

export function isChatMessage(value: unknown): value is ChatMessage {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.roomId === "string" &&
    typeof value.senderId === "string" &&
    typeof value.senderName === "string" &&
    typeof value.content === "string" &&
    isChatMessageType(value.messageType) &&
    typeof value.deleted === "boolean" &&
    typeof value.createdAt === "string";
}

function isChatRoom(value: unknown): value is ChatRoom {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.isPublic === "boolean" &&
    typeof value.ownerUserId === "string" &&
    typeof value.name === "string";
}

function isChatRoomSummary(value: unknown): value is ChatRoomSummary {
  if (!isChatRoom(value) || !isRecord(value)) return false;
  return typeof value["unreadCount"] === "number" &&
    (value["lastMessage"] === null || typeof value["lastMessage"] === "string") &&
    (value["lastMessageAt"] === null || typeof value["lastMessageAt"] === "string");
}

function isChatRoomDirectory(value: unknown): value is ChatRoomDirectory {
  return isRecord(value) &&
    Array.isArray(value.publicRooms) &&
    value.publicRooms.every(isChatRoomSummary) &&
    Array.isArray(value.privateRooms) &&
    value.privateRooms.every(isChatRoomSummary) &&
    typeof value.totalUnread === "number";
}

function isInvitedUser(value: unknown): value is InvitedUser {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.email === "string" &&
    typeof value.name === "string";
}

function isChatParticipant(value: unknown): value is ChatParticipant {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.name === "string" &&
    (value.email === null || typeof value.email === "string") &&
    typeof value.self === "boolean" &&
    typeof value.online === "boolean";
}

function isChatDirectoryUser(value: unknown): value is ChatDirectoryUser {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.name === "string" &&
    typeof value.email === "string" &&
    typeof value.online === "boolean" &&
    typeof value.self === "boolean" &&
    (value.connectedAt === null || typeof value.connectedAt === "string");
}

function isUnread(value: unknown): value is { unreadCount: number } {
  return isRecord(value) && typeof value.unreadCount === "number";
}

function isChatMessageList(value: unknown): value is ChatMessage[] {
  return Array.isArray(value) && value.every(isChatMessage);
}

function isChatParticipantList(value: unknown): value is ChatParticipant[] {
  return Array.isArray(value) && value.every(isChatParticipant);
}

function isChatDirectoryUserList(value: unknown): value is ChatDirectoryUser[] {
  return Array.isArray(value) && value.every(isChatDirectoryUser);
}

async function parseBody(response: Response): Promise<unknown> {
  return response.json().catch(() => null);
}

function errorMessage(body: unknown, status: number): string {
  return isRecord(body) && typeof body.message === "string"
    ? body.message
    : `채팅 요청에 실패했습니다. (${status})`;
}

async function get<T>(
  path: string,
  isValid: (value: unknown) => value is T,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    cache: "no-store",
    signal,
  });
  const body = (await parseBody(response)) as ApiResponse<unknown> | null;
  if (!response.ok) throw new Error(errorMessage(body, response.status));
  if (!isRecord(body) || !isValid(body.data)) {
    throw new Error("채팅 응답 형식이 올바르지 않습니다.");
  }
  return body.data;
}

async function post(path: string, payload?: unknown): Promise<unknown> {
  const csrf = await getCsrfToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: payload === undefined ? undefined : JSON.stringify(payload),
  });
  const body = (await parseBody(response)) as ApiResponse<unknown> | null;
  if (!response.ok) throw new Error(errorMessage(body, response.status));
  return isRecord(body) ? body.data : null;
}

export const chatService = {
  async findRooms(signal?: AbortSignal): Promise<ChatRoomDirectory> {
    return get("/api/v1/chat/rooms", isChatRoomDirectory, signal);
  },

  async createRoom(name: string, isPublic: boolean): Promise<ChatRoom> {
    const data = await post("/api/v1/chat/rooms", { name, isPublic });
    if (!isChatRoom(data)) throw new Error("채팅방 생성 응답 형식이 올바르지 않습니다.");
    return data;
  },

  async startConversation(email: string): Promise<ChatRoom> {
    const data = await post("/api/v1/chat/conversations", { email });
    if (!isChatRoom(data)) throw new Error("채팅방 생성 응답 형식이 올바르지 않습니다.");
    return data;
  },

  async join(roomId: string): Promise<void> {
    await post(`/api/v1/chat/rooms/${roomId}/join`);
  },

  async findParticipants(roomId: string, signal?: AbortSignal): Promise<ChatParticipant[]> {
    return get(
      `/api/v1/chat/rooms/${roomId}/participants`,
      isChatParticipantList,
      signal,
    );
  },

  async invite(roomId: string, email: string): Promise<InvitedUser> {
    const data = await post(`/api/v1/chat/rooms/${roomId}/participants`, { email });
    if (!isInvitedUser(data)) throw new Error("초대 응답 형식이 올바르지 않습니다.");
    return data;
  },

  async searchUsers(query: string, signal?: AbortSignal): Promise<ChatDirectoryUser[]> {
    return get(
      `/api/v1/chat/users?query=${encodeURIComponent(query)}&limit=30`,
      isChatDirectoryUserList,
      signal,
    );
  },

  async findOnlineUsers(signal?: AbortSignal): Promise<ChatDirectoryUser[]> {
    return get("/api/v1/chat/online-users", isChatDirectoryUserList, signal);
  },

  async leave(roomId: string): Promise<void> {
    await post(`/api/v1/chat/rooms/${roomId}/leave`);
  },

  async findMessages(
    roomId: string,
    before?: string,
    signal?: AbortSignal,
  ): Promise<ChatMessage[]> {
    const query = before ? `?before=${encodeURIComponent(before)}` : "";
    return get(`/api/v1/chat/rooms/${roomId}/messages${query}`, isChatMessageList, signal);
  },

  async unreadCount(roomId: string, signal?: AbortSignal): Promise<number> {
    const data = await get(`/api/v1/chat/rooms/${roomId}/unread`, isUnread, signal);
    return data.unreadCount;
  },

  async send(roomId: string, content: string): Promise<ChatMessage> {
    const data = await post(`/api/v1/chat/rooms/${roomId}/messages`, { content });
    if (!isChatMessage(data)) throw new Error("메시지 전송 응답 형식이 올바르지 않습니다.");
    return data;
  },

  async markRead(roomId: string): Promise<void> {
    await post(`/api/v1/chat/rooms/${roomId}/read`);
  },
};
