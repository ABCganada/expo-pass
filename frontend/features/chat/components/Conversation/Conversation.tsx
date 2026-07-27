"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, LogOut, Send, UserPlus, Users } from "lucide-react";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { useSocket } from "../../../socket/useSocket";
import type { ChatMessageType, ChatRoomSummary } from "../../types/chat";
import { useGetChatParticipantsQuery } from "../../api/chatApi";
import { useChatRoom } from "../../hooks/useChatRoom";
import { ParticipantList } from "../ParticipantList/ParticipantList";
import styles from "./Conversation.module.css";

const PARTICIPANT_REFRESH_FALLBACK_MILLIS = 5 * 60_000;
const PRESENCE_CHANGED_DESTINATION = "/topic/presence.changed";

function isMembershipMessageType(messageType: ChatMessageType): boolean {
  return messageType === "MEMBER_JOINED" ||
    messageType === "MEMBER_LEFT" ||
    messageType === "MEMBER_INVITED" ||
    messageType === "MEMBER_KICKED";
}

interface ConversationProps {
  room: ChatRoomSummary | null;
  currentUserId: string;
  onBack: () => void;
  onInvite: () => void;
  onLeave: () => void;
}

function formatTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleTimeString("ko-KR", { hour: "numeric", minute: "2-digit" });
}

export function Conversation({
  room,
  currentUserId,
  onBack,
  onInvite,
  onLeave,
}: ConversationProps) {
  const [draft, setDraft] = useState("");
  const [participantsOpen, setParticipantsOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const previousCount = useRef(0);
  const handledMembershipMessageId = useRef<string | null>(null);
  const { subscribe } = useSocket();
  const {
    messages,
    connection,
    error,
    send,
    loadOlder,
    isLoadingMore,
    hasMore,
  } = useChatRoom(room?.id ?? null);
  const participantsQuery = useGetChatParticipantsQuery(room?.id ?? "", {
    skip: room === null,
    pollingInterval: PARTICIPANT_REFRESH_FALLBACK_MILLIS,
    refetchOnFocus: true,
    refetchOnReconnect: true,
  });
  const participants = participantsQuery.data ?? [];
  const refetchParticipants = participantsQuery.refetch;

  useEffect(() => {
    const element = listRef.current;
    if (!element) return;
    const appended = messages.length - previousCount.current;
    previousCount.current = messages.length;
    if (appended > 0 && !isLoadingMore) element.scrollTop = element.scrollHeight;
  }, [messages, isLoadingMore]);

  useEffect(() => {
    if (!room) return;
    const unsubscribePresence = subscribe(
      PRESENCE_CHANGED_DESTINATION,
      () => void refetchParticipants(),
    );
    return () => {
      unsubscribePresence();
    };
  }, [refetchParticipants, room, subscribe]);

  useEffect(() => {
    const latest = messages.at(-1);
    if (!latest || handledMembershipMessageId.current === latest.id) return;
    if (!isMembershipMessageType(latest.messageType)) return;
    handledMembershipMessageId.current = latest.id;
    const timer = window.setTimeout(() => void refetchParticipants(), 0);
    return () => window.clearTimeout(timer);
  }, [messages, refetchParticipants]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const content = draft.trim();
    if (!content) return;
    setDraft("");
    await send(content);
  };

  const handleParticipantsToggle = () => {
    setParticipantsOpen((current) => !current);
  };

  const participantCount = room && !participantsQuery.isLoading
    ? participants.length
    : null;
  const participantsError = participantsQuery.error
    ? queryErrorMessage(
        participantsQuery.error,
        "참가자 목록을 불러오지 못했습니다.",
      )
    : null;

  if (!room) {
    return (
      <div className={styles.placeholder}>
        <p>왼쪽 목록에서 대화할 채팅방을 선택하세요.</p>
      </div>
    );
  }

  return (
    <section className={styles.conversation} aria-label={`${room.name} 대화`}>
      <header className={styles.header}>
        <button type="button" className={styles.back} onClick={onBack} aria-label="채팅방 목록">
          <ChevronLeft aria-hidden />
        </button>
        <div className={styles.roomInfo}>
          <strong>{room.name}</strong>
          <span data-state={connection}>
            {connection === "live" ? "실시간 연결" : connection === "connecting" ? "연결 중" : "재연결 중"}
          </span>
        </div>
        <div className={styles.actions}>
          <button
            type="button"
            className={styles.participantsButton}
            data-active={participantsOpen}
            onClick={handleParticipantsToggle}
            aria-label={
              participantCount === null ? "참가자 목록" : `참가자 목록, 현재 ${participantCount}명`
            }
          >
            <Users aria-hidden />
            <span className={styles.participantCount} aria-hidden>
              {participantCount ?? "…"}
            </span>
          </button>
          {!room.isPublic && (
            <button type="button" onClick={onInvite} aria-label="사용자 초대">
              <UserPlus aria-hidden />
            </button>
          )}
          <button type="button" onClick={onLeave} aria-label="채팅방 나가기">
            <LogOut aria-hidden />
          </button>
        </div>
      </header>

      {participantsOpen && (
        <ParticipantList
          participants={participants}
          isLoading={participantsQuery.isLoading}
          error={participantsError}
          onClose={() => setParticipantsOpen(false)}
        />
      )}

      <div ref={listRef} className={styles.messages}>
        {hasMore && messages.length > 0 && (
          <button
            type="button"
            className={styles.more}
            onClick={() => void loadOlder()}
            disabled={isLoadingMore}
          >
            {isLoadingMore ? "불러오는 중" : "이전 대화 더 보기"}
          </button>
        )}

        {messages.length === 0 ? (
          <p className={styles.empty}>{error ?? "아직 메시지가 없습니다. 첫 대화를 시작해 보세요."}</p>
        ) : (
          messages.map((message) => (
            <article
              key={message.id}
              className={styles.message}
              data-mine={message.messageType === "USER" && message.senderId === currentUserId}
              data-message-type={message.messageType}
              data-deleted={message.deleted}
            >
              {message.messageType === "USER" && (
                <span className={styles.sender}>{message.senderName}</span>
              )}
              <p className={styles.content}>{message.content}</p>
              <time className={styles.time} dateTime={message.createdAt}>
                {formatTime(message.createdAt)}
              </time>
            </article>
          ))
        )}
      </div>

      {error && messages.length > 0 && <p className={styles.error}>{error}</p>}

      <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
        <input
          className={styles.input}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="메시지를 입력하세요"
          aria-label="메시지"
          maxLength={1000}
        />
        <button
          type="submit"
          className={styles.send}
          disabled={draft.trim().length === 0}
          aria-label="메시지 전송"
        >
          <Send aria-hidden />
        </button>
      </form>
    </section>
  );
}
