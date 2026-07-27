"use client";

import { useState } from "react";
import { Globe2, Lock, X } from "lucide-react";
import { queryErrorMessage } from "@/features/store/api/queryError";
import {
  useCreateChatRoomMutation,
  useInviteChatParticipantMutation,
} from "../../api/chatApi";
import type { ChatDirectoryUser, ChatRoom } from "../../types/chat";
import { UserPicker } from "../UserPicker/UserPicker";
import styles from "./RoomActionDialog.module.css";

type DialogMode = "create" | "invite";

interface RoomActionDialogProps {
  mode: DialogMode;
  roomId?: string;
  roomName?: string;
  isAdmin: boolean;
  defaultPublic?: boolean;
  onClose: () => void;
  onComplete: (createdRoom?: ChatRoom) => void;
}

export function RoomActionDialog({
  mode,
  roomId,
  roomName,
  isAdmin,
  defaultPublic = false,
  onClose,
  onComplete,
}: RoomActionDialogProps) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [isPublic, setIsPublic] = useState(isAdmin && defaultPublic);
  const [error, setError] = useState<string | null>(null);
  const [createRoom, createState] = useCreateChatRoomMutation();
  const [inviteParticipant, inviteState] = useInviteChatParticipantMutation();
  const isSubmitting = createState.isLoading || inviteState.isLoading;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      if (mode === "create") {
        const created = await createRoom({
          name: name.trim(),
          isPublic: isAdmin && isPublic,
        }).unwrap();
        onComplete(created);
      } else {
        if (!roomId) throw new Error("초대할 채팅방이 선택되지 않았습니다.");
        await inviteParticipant({ roomId, email: email.trim() }).unwrap();
        onComplete();
      }
    } catch (reason) {
      setError(queryErrorMessage(reason, "요청을 처리하지 못했습니다."));
    }
  };

  const handleAdminSelect = (user: ChatDirectoryUser) => {
    setEmail(user.email);
    setError(null);
  };

  const invalid =
    isSubmitting ||
    (mode === "create" ? name.trim().length === 0 : email.trim().length === 0);

  return (
    <div className={styles.backdrop} role="presentation" onMouseDown={onClose}>
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label={mode === "create" ? "채팅방 만들기" : "사용자 초대"}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className={styles.header}>
          <div>
            <h2>{mode === "create" ? "채팅방 만들기" : "사용자 초대"}</h2>
            <p>
              {mode === "create"
                ? "비공개방은 초대된 사용자만 대화를 볼 수 있습니다."
                : isAdmin
                  ? `${roomName ?? "비공개방"}에 초대할 활성 사용자를 선택합니다.`
                  : `${roomName ?? "비공개방"}에 정확한 이메일로 초대합니다.`}
            </p>
          </div>
          <button type="button" className={styles.close} onClick={onClose} aria-label="닫기">
            <X aria-hidden />
          </button>
        </header>

        <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          {mode === "create" ? (
            <>
              <label className={styles.field}>
                <span>방 제목</span>
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="예: 공법 스터디"
                  maxLength={60}
                  autoFocus
                />
              </label>

              {isAdmin && (
                <fieldset className={styles.types}>
                  <legend>방 공개 범위</legend>
                  <button
                    type="button"
                    data-selected={!isPublic}
                    onClick={() => setIsPublic(false)}
                  >
                    <Lock aria-hidden />
                    <span>
                      <strong>비공개방</strong>
                      <small>초대된 사용자만</small>
                    </span>
                  </button>
                  <button
                    type="button"
                    data-selected={isPublic}
                    onClick={() => setIsPublic(true)}
                  >
                    <Globe2 aria-hidden />
                    <span>
                      <strong>공개방</strong>
                      <small>모든 로그인 사용자</small>
                    </span>
                  </button>
                </fieldset>
              )}
            </>
          ) : isAdmin ? (
            <UserPicker selectedEmail={email} onSelect={handleAdminSelect} />
          ) : (
            <label className={styles.field}>
              <span>초대할 사용자 이메일</span>
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="user@example.com"
                autoComplete="off"
                autoFocus
              />
              <small>정확히 일치하는 활성 계정만 초대할 수 있습니다.</small>
            </label>
          )}

          {error && <p className={styles.error}>{error}</p>}

          <div className={styles.actions}>
            <button type="button" className={styles.cancel} onClick={onClose}>
              취소
            </button>
            <button type="submit" className={styles.submit} disabled={invalid}>
              {isSubmitting ? "처리 중..." : mode === "create" ? "방 만들기" : "초대하기"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
