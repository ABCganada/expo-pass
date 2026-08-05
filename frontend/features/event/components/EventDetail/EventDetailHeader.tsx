"use client";

import { useState } from "react";
import { ArrowLeft, Ban } from "lucide-react";
import { useRouter } from "next/navigation";
import { usePublishAdminEventMutation, useCancelAdminEventMutation } from "../../api/adminEventDetailApi";
import { useCancelManagerEventMutation } from "../../api/managerEventDetailApi";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { AlertDialog } from "../AlertDialog/AlertDialog";
import type { AdminEventStatus } from "../../types/eventList";
import type { EventManagementDetail } from "../../types/eventManagementDetail";
import { getEventsBasePath, type EventRole } from "../../types/eventRole";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetailHeader.module.css";

const STATUS_LABEL: Record<AdminEventStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시됨",
  CANCELLED: "취소됨",
};

const PUBLISH_REQUIREMENT_MESSAGE =
  "게시하려면 주최자명, 행사 기간, 장소 정보를 모두 입력해 주세요.";

function isPublishReady(detail: EventManagementDetail): boolean {
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
  detail: EventManagementDetail;
  onStatusChanged: (message: string) => void;
  mode: EventRole;
}

type DialogState = "none" | "publishInvalid" | "confirmPublish" | "confirmCancel";

export function EventDetailHeader({ eventId, detail, onStatusChanged, mode }: EventDetailHeaderProps) {
  const router = useRouter();
  const basePath = getEventsBasePath(mode);
  const [publishAdminEvent, { isLoading: isPublishing }] = usePublishAdminEventMutation();
  const [cancelAdminEvent, { isLoading: isCancellingAsAdmin }] = useCancelAdminEventMutation();
  const [cancelManagerEvent, { isLoading: isCancellingAsManager }] = useCancelManagerEventMutation();
  const isLoading = isPublishing || isCancellingAsAdmin || isCancellingAsManager;
  const [error, setError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>("none");

  const applyPublish = async () => {
    setError(null);
    try {
      await publishAdminEvent(eventId).unwrap();
      onStatusChanged("행사가 게시되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "게시에 실패했습니다."));
    }
  };

  const applyCancel = async () => {
    setError(null);
    try {
      const cancelEvent = mode === "admin" ? cancelAdminEvent : cancelManagerEvent;
      await cancelEvent(eventId).unwrap();
      onStatusChanged("행사가 취소되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "취소에 실패했습니다."));
    }
  };

  const handlePublishClick = () => {
    setDialog(isPublishReady(detail) ? "confirmPublish" : "publishInvalid");
  };

  const handleConfirmPublish = () => {
    setDialog("none");
    void applyPublish();
  };

  const handleConfirmCancel = () => {
    setDialog("none");
    void applyCancel();
  };

  return (
    <header className={styles.header}>
      <button type="button" className={styles.backButton} onClick={() => router.push(basePath)}>
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
          {mode === "admin" && detail.status === "DRAFT" && (
            <button
              type="button"
              className={styles.publishButton}
              onClick={handlePublishClick}
              disabled={isLoading}
            >
              {isLoading ? "처리 중..." : "게시하기"}
            </button>
          )}
          {detail.status === "PUBLISHED" && detail.phase !== "ENDED" && (
            <button
              type="button"
              className={styles.cancelStatusButton}
              onClick={() => setDialog("confirmCancel")}
              disabled={isLoading}
            >
              <Ban size={16} />
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