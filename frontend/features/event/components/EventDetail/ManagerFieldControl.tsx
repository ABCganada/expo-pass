"use client";

import { useState } from "react";
import { useChangeAdminEventManagerMutation } from "../../api/adminEventApi";
import { ManagerPicker, type ManagerOption } from "../ManagerPicker/ManagerPicker";
import type { EventManagementDetail } from "../../types/eventManagementDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./ManagerFieldControl.module.css";

interface ManagerFieldControlProps {
  eventId: string;
  detail: EventManagementDetail;
  onSaved: (message: string) => void;
}

function toManagerOption(detail: EventManagementDetail): ManagerOption | null {
  return detail.managerId ? { id: detail.managerId, name: detail.managerName ?? "", email: "" } : null;
}

function displayManager(manager: ManagerOption | null): string {
  if (!manager) return "-";
  return manager.email ? `${manager.name} (${manager.email})` : manager.name;
}

/** 담당자 필드 하나만 담당하는 slot 컴포넌트. changeManager mutation은 여기서만 호출한다. */
export function ManagerFieldControl({ eventId, detail, onSaved }: ManagerFieldControlProps) {
  const [changeManager, { isLoading }] = useChangeAdminEventManagerMutation();
  const [isEditing, setIsEditing] = useState(false);
  const [manager, setManager] = useState<ManagerOption | null>(() => toManagerOption(detail));
  const [error, setError] = useState<string | null>(null);

  // 다른 행사로 전환되거나 저장 후 재조회되면(managerId 변경) 대기 중인 선택값도 서버 상태로 재동기화한다.
  const [syncedManagerId, setSyncedManagerId] = useState(detail.managerId);
  if (detail.managerId !== syncedManagerId) {
    setSyncedManagerId(detail.managerId);
    setManager(toManagerOption(detail));
    setIsEditing(false);
    setError(null);
  }

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

  if (isEditing) {
    return (
      <>
        <ManagerPicker selectedManager={manager} onSelect={setManager} onClear={() => setManager(null)} />
        {error && <p className={styles.fieldError}>{error}</p>}
        <div className={styles.managerEditActions}>
          <button type="button" className={styles.managerCancelButton} onClick={handleCancel} disabled={isLoading}>
            취소
          </button>
          <button
            type="button"
            className={styles.managerSaveButton}
            onClick={() => void handleSave()}
            disabled={isLoading}
          >
            {isLoading ? "저장 중..." : "저장"}
          </button>
        </div>
      </>
    );
  }

  return (
    <div className={styles.managerRow}>
      <div className={styles.readonlyBox}>{displayManager(manager)}</div>
      <button type="button" className={styles.changeButton} onClick={handleStartEdit}>
        변경
      </button>
    </div>
  );
}