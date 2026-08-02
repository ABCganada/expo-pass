"use client";

import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { useChangeAdminEventStatusMutation } from "../../api/adminEventDetailApi";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import type { AdminEventStatus } from "../../types/adminEvent";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetailHeader.module.css";

const STATUS_OPTIONS: { value: AdminEventStatus; label: string }[] = [
  { value: "DRAFT", label: "DRAFT" },
  { value: "PUBLISHED", label: "PUBLISHED" },
  { value: "CANCELLED", label: "CANCELLED" },
];

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

  const handleStatusChange = async (next: AdminEventStatus) => {
    if (next === status || isLoading) return;
    setError(null);
    try {
      await changeStatus({ eventId, status: next }).unwrap();
      onStatusChanged("상태가 변경되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "상태 변경에 실패했습니다."));
    }
  };

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <button type="button" className={styles.backButton} onClick={() => router.push("/admin/events")}>
          <ArrowLeft size={16} />
          목록으로
        </button>
        <h1 className={styles.title}>{title}</h1>
      </div>
      <div className={styles.right}>
        <StatusFilterDropdown options={STATUS_OPTIONS} value={status} onChange={(next) => void handleStatusChange(next)} />
        {error && <p className={styles.error}>{error}</p>}
      </div>
    </header>
  );
}