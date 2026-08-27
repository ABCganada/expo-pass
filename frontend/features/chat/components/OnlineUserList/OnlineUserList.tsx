"use client";

import { MessageCircle } from "lucide-react";
import type { ChatDirectoryUser } from "../../types/chat";
import styles from "./OnlineUserList.module.css";

interface OnlineUserListProps {
  users: ChatDirectoryUser[];
  isLoading: boolean;
  busyUserId: string | null;
  onStartConversation: (user: ChatDirectoryUser) => void;
}

export function OnlineUserList({
  users,
  isLoading,
  busyUserId,
  onStartConversation,
}: OnlineUserListProps) {
  if (isLoading) return <p className={styles.state}>접속자를 불러오는 중...</p>;
  if (users.length === 0) return <p className={styles.state}>현재 접속 중인 다른 사용자가 없습니다.</p>;

  return (
    <ul className={styles.list}>
      {users.map((user) => (
        <li key={user.id}>
          <span className={styles.dot} aria-hidden />
          <div className={styles.identity}>
            <strong>
              {user.name}
              {user.self && <em>나</em>}
            </strong>
            <small>{user.email}</small>
          </div>
          {user.self ? (
            <span className={styles.selfStatus}>현재 접속</span>
          ) : (
            <button
              type="button"
              onClick={() => onStartConversation(user)}
              disabled={busyUserId !== null}
              aria-label={`${user.name}님과 비공개방 만들기`}
            >
              <MessageCircle aria-hidden />
              {busyUserId === user.id ? "생성 중" : "대화 시작"}
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}
