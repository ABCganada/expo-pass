"use client";

import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { useChangeAdminEventStatusMutation } from "../../api/adminEventDetailApi";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import type { AdminEventStatus } from "../../types/adminEvent";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetailHeader.module.css";

const STATUS_LABEL: Record<AdminEventStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시됨",
  CANCELLED: "취소됨",
};

interface EventDetailHeaderProps {
  eventId: string;
  title: string;
  status: AdminEventStatus;
  onStatusChanged: (message: string) => void;
}

export function EventDetailHeader({ eventId, title, status, onStatusChanged }: EventDetailHeaderProps) {
  const router = useRouter();
  const [changeStatus, { isLoading }] = useChangeAdminEventStatusMutation();
  const [error, setError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);

  const applyStatus = async (next: AdminEventStatus) => {
    setError(null);
    try {
      await changeStatus({ eventId, status: next }).unwrap();
      onStatusChanged(next === "PUBLISHED" ? "행사가 게시되었습니다." : "행사가 취소되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "상태 변경에 실패했습니다."));
    }
  };

  const handleCancelConfirm = () => {
    setConfirmCancel(false);
    void applyStatus("CANCELLED");
  };

  return (
    <header className={styles.header}>
      <button type="button" className={styles.backButton} onClick={() => router.push("/admin/events")}>
        <ArrowLeft size={16} />
        목록으로
      </button>

      <div className={styles.titleRow}>
        <div className={styles.titleGroup}>
          <h1 className={styles.title}>{title}</h1>
          <span className={styles.statusBadge} data-status={status}>
            {STATUS_LABEL[status]}
          </span>
        </div>

        <div className={styles.actions}>
          {status === "DRAFT" && (
            <button
              type="button"
              className={styles.publishButton}
              onClick={() => void applyStatus("PUBLISHED")}
              disabled={isLoading}
            >
              {isLoading ? "처리 중..." : "게시하기"}
            </button>
          )}
          {status === "PUBLISHED" && (
            <button
              type="button"
              className={styles.cancelStatusButton}
              onClick={() => setConfirmCancel(true)}
              disabled={isLoading}
            >
              취소하기
            </button>
          )}
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {confirmCancel && (
        <ConfirmDialog
          title="행사를 취소할까요?"
          description="취소하면 되돌릴 수 없습니다."
          confirmLabel="취소하기"
          danger
          onConfirm={handleCancelConfirm}
          onCancel={() => setConfirmCancel(false)}
        />
      )}
    </header>
  );
}