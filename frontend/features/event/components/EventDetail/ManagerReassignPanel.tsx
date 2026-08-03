"use client";

import { useState } from "react";
import { useChangeAdminEventManagerMutation } from "../../api/adminEventApi";
import { ManagerPicker, type ManagerOption } from "../ManagerPicker/ManagerPicker";
import type { EventManagementDetail } from "../../types/eventManagementDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetail.module.css";

interface ManagerReassignPanelProps {
  eventId: string;
  detail: EventManagementDetail;
  onSaved: (message: string) => void;
}

function toManagerOption(detail: EventManagementDetail): ManagerOption | null {
  return detail.managerId ? { id: detail.managerId, name: detail.managerName ?? "", email: "" } : null;
}

export function ManagerReassignPanel({ eventId, detail, onSaved }: ManagerReassignPanelProps) {
  const [changeManager, { isLoading }] = useChangeAdminEventManagerMutation();
  const [isEditing, setIsEditing] = useState(false);
  const [manager, setManager] = useState<ManagerOption | null>(() => toManagerOption(detail));
  const [error, setError] = useState<string | null>(null);

  const handleStartEdit = () => {
    setManager(toManagerOption(detail));
    setError(null);
    setIsEditing(true);
  };

  const handleCancel = () => {
    setManager(toManagerOption(detail));
    setError(null);
    setIsEditing(false);
  };

  const handleSave = async () => {
    if (!manager) {
      setError("담당자를 선택해주세요.");
      return;
    }
    if (manager.id === detail.managerId) {
      setIsEditing(false);
      return;
    }
    setError(null);
    try {
      await changeManager({ eventId, managerId: manager.id }).unwrap();
      onSaved("담당자를 변경했습니다.");
      setIsEditing(false);
    } catch (reason) {
      setError(queryErrorMessage(reason, "담당자 변경에 실패했습니다."));
    }
  };

  return (
    <div className={styles.form}>
      <label className={styles.managerLabel}>
        <span>담당자</span>
        {isEditing ? (
          <ManagerPicker selectedManager={manager} onSelect={setManager} onClear={() => setManager(null)} />
        ) : (
          <div className={styles.readonlyBox}>{detail.managerName ?? "-"}</div>
        )}
      </label>

      {error && <p className={styles.error}>{error}</p>}

      {isEditing ? (
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isLoading}>
            취소
          </button>
          <button type="button" className={styles.saveButton} onClick={() => void handleSave()} disabled={isLoading}>
            {isLoading ? "저장 중..." : "저장"}
          </button>
        </div>
      ) : (
        <div className={styles.viewHeader}>
          <button type="button" className={styles.editButton} onClick={handleStartEdit}>
            담당자 변경
          </button>
        </div>
      )}
    </div>
  );
}