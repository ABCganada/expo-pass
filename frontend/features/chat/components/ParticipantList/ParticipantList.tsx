"use client";

import { X } from "lucide-react";
import type { ChatParticipant } from "../../types/chat";
import styles from "./ParticipantList.module.css";

interface ParticipantListProps {
  participants: ChatParticipant[];
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
}

export function ParticipantList({
  participants,
  isLoading,
  error,
  onClose,
}: ParticipantListProps) {
  return (
    <aside className={styles.panel} aria-label="채팅방 참가자">
      <header className={styles.header}>
        <div>
          <strong>참가자</strong>
          <span>{participants.length}명</span>
        </div>
        <button type="button" onClick={onClose} aria-label="참가자 목록 닫기">
          <X aria-hidden />
        </button>
      </header>

      {isLoading ? (
        <p className={styles.state}>참가자를 불러오는 중...</p>
      ) : error ? (
        <p className={styles.error}>{error}</p>
      ) : participants.length === 0 ? (
        <p className={styles.state}>현재 참가자가 없습니다.</p>
      ) : (
        <ul className={styles.list}>
          {participants.map((participant) => (
            <li key={participant.id} data-online={participant.online}>
              <span className={styles.dot} aria-hidden />
              <div className={styles.identity}>
                <strong>
                  {participant.name}
                  {participant.self && <em>나</em>}
                </strong>
                {participant.email && <small>{participant.email}</small>}
              </div>
              <span className={styles.status}>
                {participant.online ? "온라인" : "오프라인"}
              </span>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
