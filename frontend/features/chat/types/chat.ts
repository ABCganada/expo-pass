export const CHAT_MESSAGE_TYPES = [
  "USER",
  "MEMBER_JOINED",
  "MEMBER_LEFT",
  "MEMBER_INVITED",
  "MEMBER_KICKED",
  "ROOM_RENAMED",
] as const;

export type ChatMessageType = (typeof CHAT_MESSAGE_TYPES)[number];

export interface ChatMessage {
  id: string;
  roomId: string;
  senderId: string;
  senderName: string;
  content: string;
  messageType: ChatMessageType;
  deleted: boolean;
  createdAt: string;
}

export interface ChatRoom {
  id: string;
  isPublic: boolean;
  ownerUserId: string;
  name: string;
}

export interface ChatRoomSummary extends ChatRoom {
  unreadCount: number;
  lastMessage: string | null;
  lastMessageAt: string | null;
}

export interface ChatRoomDirectory {
  publicRooms: ChatRoomSummary[];
  privateRooms: ChatRoomSummary[];
  totalUnread: number;
}

export interface InvitedUser {
  id: string;
  email: string;
  name: string;
}

export interface ChatParticipant {
  id: string;
  name: string;
  /** 일반 사용자의 참가자 조회에서는 개인정보 보호를 위해 null이다. */
  email: string | null;
  self: boolean;
  online: boolean;
}

/** 관리자에게만 공개되는 사용자 선택·접속 현황 정보. */
export interface ChatDirectoryUser {
  id: string;
  name: string;
  email: string;
  online: boolean;
  self: boolean;
  connectedAt: string | null;
}
