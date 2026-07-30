import { Users } from "lucide-react";
import styles from "./WaitingRoomPanel.module.css";

interface WaitingRoomPanelProps {
  position: number | null;
}

export function WaitingRoomPanel({ position }: WaitingRoomPanelProps) {
  return (
    <div className={styles.panel}>
      <div className={styles.spinner} aria-hidden="true" />
      <p className={styles.title}>대기열에 입장했습니다</p>
      <p className={styles.description}>
        접속자가 많아 순서대로 입장하고 있어요. 창을 닫지 말고 잠시만 기다려주세요.
      </p>
      <div className={styles.positionBox}>
        <Users size={18} />
        <span>
          내 앞에 <strong>{position ?? "-"}</strong>명 대기 중
        </span>
      </div>
    </div>
  );
}
