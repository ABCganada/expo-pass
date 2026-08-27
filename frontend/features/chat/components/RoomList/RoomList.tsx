import { Globe2, Lock } from "lucide-react";
import type { ChatRoomSummary } from "../../types/chat";
import styles from "./RoomList.module.css";

interface RoomListProps {
  rooms: ChatRoomSummary[];
  selectedRoomId: string | null;
  emptyMessage: string;
  onSelect: (roomId: string) => void;
}

function formatRoomTime(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const today = new Date();
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
  }
  return date.toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" });
}

function badgeText(count: number): string {
  return count > 99 ? "99+" : String(count);
}

export function RoomList({
  rooms,
  selectedRoomId,
  emptyMessage,
  onSelect,
}: RoomListProps) {
  if (rooms.length === 0) {
    return <p className={styles.empty}>{emptyMessage}</p>;
  }

  return (
    <div className={styles.list}>
      {rooms.map((room) => (
        <button
          key={room.id}
          type="button"
          className={styles.room}
          data-selected={room.id === selectedRoomId}
          onClick={() => onSelect(room.id)}
        >
          <span className={styles.icon} data-public={room.isPublic}>
            {room.isPublic ? <Globe2 aria-hidden /> : <Lock aria-hidden />}
          </span>
          <span className={styles.body}>
            <span className={styles.titleRow}>
              <strong className={styles.title}>{room.name}</strong>
              <time className={styles.time} dateTime={room.lastMessageAt ?? undefined}>
                {formatRoomTime(room.lastMessageAt)}
              </time>
            </span>
            <span className={styles.previewRow}>
              <span className={styles.preview}>
                {room.lastMessage ?? (room.isPublic ? "공개 대화를 시작해 보세요." : "아직 메시지가 없습니다.")}
              </span>
              {room.unreadCount > 0 && (
                <span className={styles.badge}>{badgeText(room.unreadCount)}</span>
              )}
            </span>
          </span>
        </button>
      ))}
    </div>
  );
}
