"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarDays, Plus, Settings, Trash2 } from "lucide-react";
import { useGetAdminEventsQuery } from "@/features/event/api/adminEventApi";
import { useDeleteEventMutation } from "@/features/event/api/eventCreateApi";
import { ConfirmDialog } from "@/features/event/components/ConfirmDialog/ConfirmDialog";
import { Toast } from "@/features/event/components/Toast/Toast";
import type { AdminEventStatus } from "@/features/event/types/adminEvent";
import { formatPeriod } from "@/features/event/utils/eventPhase";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./page.module.css";

const STATUS_LABEL: Record<AdminEventStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시됨",
  CANCELLED: "취소",
};

const STATUS_TAB_VALUES: (AdminEventStatus | "ALL")[] = ["ALL", "DRAFT", "PUBLISHED", "CANCELLED"];

function statusTabLabel(value: AdminEventStatus | "ALL"): string {
  return value === "ALL" ? "전체" : STATUS_LABEL[value];
}

export default function AdminEventsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const isAdmin = user?.roles.includes("ADMIN") ?? false;

  const [statusFilter, setStatusFilter] = useState<AdminEventStatus | "ALL">("ALL");
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const { data: events = [], isLoading, isFetching, isError, error } = useGetAdminEventsQuery(
    statusFilter === "ALL" ? undefined : statusFilter,
  );
  const [deleteEvent, { isLoading: isDeleting }] = useDeleteEventMutation();

  const goToDetail = (eventId: string) => router.push(`/admin/events/${eventId}`);

  const handleConfirmDelete = async () => {
    if (!pendingDeleteId) return;
    setDeleteError(null);
    try {
      await deleteEvent(pendingDeleteId).unwrap();
      setToast("행사를 삭제했습니다.");
    } catch (reason) {
      setDeleteError(queryErrorMessage(reason, "삭제에 실패했습니다."));
    } finally {
      setPendingDeleteId(null);
    }
  };

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div className={styles.titleGroup}>
          <h1 className={styles.title}>박람회 관리</h1>
        </div>
        <div className={styles.headerActions}>
          {isAdmin && (
            <button type="button" className={styles.primaryButton} onClick={() => router.push("/admin/events/new")}>
              <Plus size={16} />새 행사 등록
            </button>
          )}
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => router.push("/admin/events/categories")}
          >
            <Settings size={16} />
            카테고리 관리
          </button>
        </div>
      </header>

      <div className={styles.listSection}>
        <div className={styles.filterRow}>
          <div className={styles.statusTabs} role="tablist" aria-label="행사 상태 필터">
            {STATUS_TAB_VALUES.map((value) => (
              <button
                key={value}
                type="button"
                role="tab"
                aria-selected={statusFilter === value}
                className={styles.statusTab}
                data-active={statusFilter === value}
                onClick={() => setStatusFilter(value)}
              >
                {statusTabLabel(value)}
              </button>
            ))}
          </div>
          <span className={styles.countText}>전체 {events.length}건</span>
        </div>

        {deleteError && <p className={styles.deleteError}>{deleteError}</p>}

        {isError ? (
          <div className={styles.state}>{queryErrorMessage(error, "행사 목록을 불러오지 못했습니다.")}</div>
        ) : isLoading ? (
          <div className={styles.state}>불러오는 중...</div>
        ) : events.length === 0 ? (
          <div className={styles.state}>
            <CalendarDays size={40} className={styles.emptyIcon} />
            표시할 박람회가 없습니다.
          </div>
        ) : (
          <div className={styles.tableWrap} data-fetching={isFetching}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th className={styles.titleCol}>행사명</th>
                  <th className={styles.periodCol}>기간</th>
                  <th className={styles.statusCol}>상태</th>
                  <th className={styles.createdCol}>등록일</th>
                  {isAdmin && <th className={styles.manageCol} aria-label="관리" />}
                </tr>
              </thead>
              <tbody>
                {events.map((event) => (
                  <tr
                    key={event.id}
                    tabIndex={0}
                    className={styles.row}
                    onClick={() => goToDetail(event.id)}
                    onKeyDown={(keyEvent) => {
                      if (keyEvent.key === "Enter") goToDetail(event.id);
                    }}
                  >
                    <td>
                      <div className={styles.eventTitle}>{event.title}</div>
                      <div className={styles.eventCategory}>{event.categoryName}</div>
                    </td>
                    <td>
                      {event.startDate && event.endDate ? formatPeriod(event.startDate, event.endDate) : "미정"}
                    </td>
                    <td>
                      <span className={styles.statusBadge} data-status={event.status}>
                        {STATUS_LABEL[event.status]}
                      </span>
                    </td>
                    <td>{new Date(event.createdAt).toLocaleDateString("ko-KR")}</td>
                    {isAdmin && (
                      <td className={styles.manageCol}>
                        <button
                          type="button"
                          className={styles.deleteButton}
                          aria-label="행사 삭제"
                          disabled={isDeleting}
                          onClick={(clickEvent) => {
                            clickEvent.stopPropagation();
                            setPendingDeleteId(event.id);
                          }}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {pendingDeleteId && (
        <ConfirmDialog
          title="행사를 삭제하시겠습니까?"
          description="삭제된 행사는 복구할 수 없습니다."
          confirmLabel="삭제"
          danger
          onConfirm={() => void handleConfirmDelete()}
          onCancel={() => setPendingDeleteId(null)}
        />
      )}

      {toast && <Toast message={toast} onDismiss={() => setToast(null)} />}
    </section>
  );
}