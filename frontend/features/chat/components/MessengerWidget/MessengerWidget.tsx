"use client";

import { useEffect, useMemo, useState } from "react";
import { MessageCircle, Plus, X } from "lucide-react";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { useSocket } from "../../../socket/useSocket";
import type {
  ChatDirectoryUser,
  ChatRoom,
  ChatRoomDirectory,
} from "../../types/chat";
import {
  useGetChatRoomsQuery,
  useGetOnlineUsersQuery,
  useJoinChatRoomMutation,
  useLeaveChatRoomMutation,
  useStartConversationMutation,
} from "../../api/chatApi";
import { Conversation } from "../Conversation/Conversation";
import { OnlineUserList } from "../OnlineUserList/OnlineUserList";
import { RoomActionDialog } from "../RoomActionDialog/RoomActionDialog";
import { RoomList } from "../RoomList/RoomList";
import styles from "./MessengerWidget.module.css";

const STATE_REFRESH_FALLBACK_MILLIS = 5 * 60_000;
const CHAT_DIRECTORY_CHANGED_DESTINATION = "/topic/chat-directory.changed";
const USER_CHAT_CHANGED_DESTINATION = "/user/queue/chat.changed";
const PRESENCE_CHANGED_DESTINATION = "/topic/presence.changed";
const EMPTY_DIRECTORY: ChatRoomDirectory = {
  publicRooms: [],
  privateRooms: [],
  totalUnread: 0,
};

type RoomTab = "public" | "private" | "online";
type DialogMode = "create" | "invite" | null;

interface MessengerWidgetProps {
  currentUserId: string;
  roles: string[];
  mode: "user" | "admin";
}

function badgeText(count: number): string {
  return count > 99 ? "99+" : String(count);
}

export function MessengerWidget({ currentUserId, roles, mode }: MessengerWidgetProps) {
  const isAdmin = mode === "admin" && roles.includes("ADMIN");
  const { subscribe } = useSocket();
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<RoomTab>("private");
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [dialogMode, setDialogMode] = useState<DialogMode>(null);
  const roomsQuery = useGetChatRoomsQuery(undefined, {
    pollingInterval: STATE_REFRESH_FALLBACK_MILLIS,
    refetchOnFocus: true,
    refetchOnReconnect: true,
  });
  const onlineUsersQuery = useGetOnlineUsersQuery(undefined, {
    skip: !isAdmin,
    pollingInterval: STATE_REFRESH_FALLBACK_MILLIS,
    refetchOnFocus: true,
    refetchOnReconnect: true,
  });
  const [joinRoom] = useJoinChatRoomMutation();
  const [leaveRoom] = useLeaveChatRoomMutation();
  const [startConversation] = useStartConversationMutation();
  const directory = roomsQuery.data ?? EMPTY_DIRECTORY;
  const onlineUsers = onlineUsersQuery.data ?? [];
  const refetchRooms = roomsQuery.refetch;
  const refetchOnlineUsers = onlineUsersQuery.refetch;
  const isLoading = roomsQuery.isLoading;
  const isOnlineLoading = onlineUsersQuery.isLoading;
  const queryError = roomsQuery.error
    ? queryErrorMessage(roomsQuery.error, "채팅방 목록을 불러오지 못했습니다.")
    : onlineUsersQuery.error
      ? queryErrorMessage(
          onlineUsersQuery.error,
          "접속자 목록을 불러오지 못했습니다.",
        )
      : null;
  const error = actionError ?? queryError;

  useEffect(() => {
    const unsubscribePublic = subscribe(
      CHAT_DIRECTORY_CHANGED_DESTINATION,
      () => void refetchRooms(),
    );
    const unsubscribeUser = subscribe(
      USER_CHAT_CHANGED_DESTINATION,
      () => void refetchRooms(),
    );
    return () => {
      unsubscribePublic();
      unsubscribeUser();
    };
  }, [refetchRooms, subscribe]);

  useEffect(() => {
    if (!isAdmin) return;
    const unsubscribe = subscribe(
      PRESENCE_CHANGED_DESTINATION,
      () => void refetchOnlineUsers(),
    );
    return () => {
      unsubscribe();
    };
  }, [isAdmin, refetchOnlineUsers, subscribe]);

  const allRooms = useMemo(
    () => [...directory.publicRooms, ...directory.privateRooms],
    [directory],
  );
  const selectedRoom = allRooms.find((room) => room.id === selectedRoomId) ?? null;
  const visibleRooms = activeTab === "public" ? directory.publicRooms : directory.privateRooms;
  const publicUnread = directory.publicRooms
    .reduce((total, room) => total + room.unreadCount, 0);
  const privateUnread = directory.privateRooms
    .reduce((total, room) => total + room.unreadCount, 0);

  const handleSelect = async (roomId: string) => {
    const room = allRooms.find((candidate) => candidate.id === roomId);
    if (!room) return;
    setNotice(null);
    setActionError(null);
    try {
      if (room.isPublic) {
        await joinRoom(room.id).unwrap();
        await refetchRooms();
      }
      setSelectedRoomId(room.id);
    } catch (reason) {
      setActionError(queryErrorMessage(reason, "채팅방에 참여하지 못했습니다."));
    }
  };

  const handleTabChange = (tab: RoomTab) => {
    setActiveTab(tab);
    setSelectedRoomId(null);
    setNotice(null);
  };

  const handleDialogComplete = async (createdRoom?: ChatRoom) => {
    setDialogMode(null);
    if (createdRoom) {
      setActiveTab(createdRoom.isPublic ? "public" : "private");
      setSelectedRoomId(createdRoom.id);
      setNotice("채팅방을 만들었습니다.");
    } else {
      setNotice("사용자를 초대했습니다.");
    }
    await refetchRooms();
  };

  const handleLeave = async () => {
    if (!selectedRoom) return;
    if (!window.confirm(`"${selectedRoom.name}" 채팅방에서 나가시겠습니까?`)) return;
    try {
      await leaveRoom(selectedRoom.id).unwrap();
      setSelectedRoomId(null);
      setNotice("채팅방에서 퇴장하였습니다.");
      await refetchRooms();
    } catch (reason) {
      setActionError(queryErrorMessage(reason, "채팅방에서 나가지 못했습니다."));
    }
  };

  const handleStartConversation = async (user: ChatDirectoryUser) => {
    setBusyUserId(user.id);
    setActionError(null);
    try {
      const room = await startConversation(user.email).unwrap();
      await refetchRooms();
      setActiveTab("private");
      setSelectedRoomId(room.id);
      setNotice(`${user.name}님과의 비공개방을 만들었습니다.`);
    } catch (reason) {
      setActionError(queryErrorMessage(reason, "비공개방을 만들지 못했습니다."));
    } finally {
      setBusyUserId(null);
    }
  };

  const handleOpen = () => {
    setIsOpen(true);
    void refetchRooms();
  };

  return (
    <div className={styles.root}>
      {isOpen && (
        <section
          className={styles.panel}
          data-conversation-open={selectedRoom !== null}
          role="dialog"
          aria-label="메신저"
        >
          <header className={styles.panelHeader}>
            <div>
              <strong>메신저</strong>
              <span>공개방과 초대받은 비공개방</span>
            </div>
            <button type="button" onClick={() => setIsOpen(false)} aria-label="메신저 닫기">
              <X aria-hidden />
            </button>
          </header>

          {(notice || error) && (
            <p className={styles.feedback} data-error={Boolean(error)}>
              {error ?? notice}
            </p>
          )}

          <div className={styles.body}>
            <aside className={styles.directory}>
              <div className={styles.tabs} role="tablist" aria-label="채팅방 분류">
                <button
                  type="button"
                  role="tab"
                  aria-selected={activeTab === "public"}
                  data-selected={activeTab === "public"}
                  onClick={() => handleTabChange("public")}
                >
                  공개방
                  {publicUnread > 0 && (
                    <span className={styles.tabBadge} aria-label={`안 읽은 메시지 ${publicUnread}개`}>
                      {badgeText(publicUnread)}
                    </span>
                  )}
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={activeTab === "private"}
                  data-selected={activeTab === "private"}
                  onClick={() => handleTabChange("private")}
                >
                  비공개방
                  {privateUnread > 0 && (
                    <span className={styles.tabBadge} aria-label={`안 읽은 메시지 ${privateUnread}개`}>
                      {badgeText(privateUnread)}
                    </span>
                  )}
                </button>
                {isAdmin && (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === "online"}
                    data-selected={activeTab === "online"}
                    onClick={() => handleTabChange("online")}
                  >
                    접속자
                    <span className={styles.tabCount}>{onlineUsers.length}</span>
                  </button>
                )}
                <button
                  type="button"
                  className={styles.create}
                  onClick={() => setDialogMode("create")}
                  aria-label="채팅방 만들기"
                >
                  <Plus aria-hidden />
                </button>
              </div>

              {activeTab === "online" ? (
                <OnlineUserList
                  users={onlineUsers}
                  isLoading={isOnlineLoading}
                  busyUserId={busyUserId}
                  onStartConversation={(user) => void handleStartConversation(user)}
                />
              ) : isLoading ? (
                <p className={styles.loading}>채팅방을 불러오는 중...</p>
              ) : (
                <RoomList
                  rooms={visibleRooms}
                  selectedRoomId={selectedRoomId}
                  emptyMessage={
                    activeTab === "public"
                      ? "아직 활성 공개방이 없습니다."
                      : "초대받았거나 직접 만든 비공개방이 없습니다."
                  }
                  onSelect={(roomId) => void handleSelect(roomId)}
                />
              )}
            </aside>

            <Conversation
              room={selectedRoom}
              currentUserId={currentUserId}
              onBack={() => setSelectedRoomId(null)}
              onInvite={() => setDialogMode("invite")}
              onLeave={() => void handleLeave()}
            />
          </div>
        </section>
      )}

      <button
        type="button"
        className={styles.launcher}
        onClick={() => (isOpen ? setIsOpen(false) : handleOpen())}
        aria-label={isOpen ? "메신저 닫기" : "메신저 열기"}
      >
        {isOpen ? <X aria-hidden /> : <MessageCircle aria-hidden />}
        {!isOpen && directory.totalUnread > 0 && (
          <span className={styles.badge}>{badgeText(directory.totalUnread)}</span>
        )}
      </button>

      {dialogMode && (
        <RoomActionDialog
          mode={dialogMode}
          roomId={selectedRoom?.id}
          roomName={selectedRoom?.name}
          isAdmin={isAdmin}
          defaultPublic={activeTab === "public"}
          onClose={() => setDialogMode(null)}
          onComplete={(createdRoom) => void handleDialogComplete(createdRoom)}
        />
      )}
    </div>
  );
}
