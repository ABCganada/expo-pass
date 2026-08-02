"use client";

import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { useChangeAdminEventStatusMutation } from "../../api/adminEventDetailApi";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { AlertDialog } from "../AlertDialog/AlertDialog";
import type { AdminEventStatus } from "../../types/adminEvent";
import type { AdminEventDetail } from "../../types/adminEventDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetailHeader.module.css";

const STATUS_LABEL: Record<AdminEventStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시됨",
  CANCELLED: "취소됨",
};

const PUBLISH_REQUIREMENT_MESSAGE =
  "게시하려면 주최자명, 행사 기간, 장소 정보를 모두 입력해 주세요.";

function isPublishReady(detail: AdminEventDetail): boolean {
  return Boolean(
    detail.hostName &&
      detail.venueName &&
      detail.address &&
      detail.legalDongCode &&
      detail.latitude !== null &&
      detail.longitude !== null &&
      detail.startDate &&
      detail.endDate,
  );
}

interface EventDetailHeaderProps {
  eventId: string;
  detail: AdminEventDetail;
  onStatusChanged: (message: string) => void;
}

type DialogState = "none" | "publishInvalid" | "confirmPublish" | "confirmCancel";

export function EventDetailHeader({ eventId, detail, onStatusChanged }: EventDetailHeaderProps) {
  const router = useRouter();
  const [changeStatus, { isLoading }] = useChangeAdminEventStatusMutation();
  const [error, setError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>("none");

  const applyStatus = async (next: AdminEventStatus) => {
    setError(null);
    try {
      await changeStatus({ eventId, status: next }).unwrap();
      onStatusChanged(next === "PUBLISHED" ? "행사가 게시되었습니다." : "행사가 취소되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "상태 변경에 실패했습니다."));
    }
  };

  const handlePublishClick = () => {
    setDialog(isPublishReady(detail) ? "confirmPublish" : "publishInvalid");
  };

  const handleConfirmPublish = () => {
    setDialog("none");
    void applyStatus("PUBLISHED");
  };

  const handleConfirmCancel = () => {
    setDialog("none");
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
          <h1 className={styles.title}>{detail.title}</h1>
          <span className={styles.statusBadge} data-status={detail.status}>
            {STATUS_LABEL[detail.status]}
          </span>
        </div>

        <div className={styles.actions}>
          {detail.status === "DRAFT" && (
            <button
              type="button"
              className={styles.publishButton}
              onClick={handlePublishClick}
              disabled={isLoading}
            >
              {isLoading ? "처리 중..." : "게시하기"}
            </button>
          )}
          {detail.status === "PUBLISHED" && (
            <button
              type="button"
              className={styles.cancelStatusButton}
              onClick={() => setDialog("confirmCancel")}
              disabled={isLoading}
            >
              취소하기
            </button>
          )}
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {dialog === "publishInvalid" && (
        <AlertDialog
          title="게시할 수 없습니다"
          description={PUBLISH_REQUIREMENT_MESSAGE}
          onConfirm={() => setDialog("none")}
        />
      )}

      {dialog === "confirmPublish" && (
        <ConfirmDialog
          title="행사를 게시하시겠습니까?"
          description="게시 후 고객에게 노출됩니다."
          confirmLabel="게시하기"
          onConfirm={handleConfirmPublish}
          onCancel={() => setDialog("none")}
        />
      )}

      {dialog === "confirmCancel" && (
        <ConfirmDialog
          title="행사를 취소할까요?"
          description="취소하면 되돌릴 수 없습니다."
          confirmLabel="취소하기"
          danger
          onConfirm={handleConfirmCancel}
          onCancel={() => setDialog("none")}
        />
      )}
    </header>
  );
}