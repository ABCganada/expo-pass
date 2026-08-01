"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarDays, Plus, Settings } from "lucide-react";
import { useGetAdminEventsQuery } from "@/features/event/api/adminEventApi";
import { StatusFilterDropdown } from "@/features/event/components/StatusFilterDropdown/StatusFilterDropdown";
import type { AdminEventStatus } from "@/features/event/types/adminEvent";
import { formatPeriod } from "@/features/event/utils/eventPhase";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./page.module.css";

const STATUS_OPTIONS: { value: AdminEventStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "DRAFT", label: "DRAFT" },
  { value: "PUBLISHED", label: "PUBLISHED" },
  { value: "CANCELLED", label: "CANCELLED" },
];

const STATUS_LABEL: Record<AdminEventStatus, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시됨",
  CANCELLED: "취소됨",
};

export default function AdminEventsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const isAdmin = user?.roles.includes("ADMIN") ?? false;

  const [statusFilter, setStatusFilter] = useState<AdminEventStatus | "ALL">("ALL");

  const { data: events = [], isLoading, isFetching, isError, error } = useGetAdminEventsQuery(
    statusFilter === "ALL" ? undefined : statusFilter,
  );

  const goToDetail = (eventId: string) => router.push(`/admin/events/${eventId}`);

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
          <StatusFilterDropdown
            label="상태:"
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={setStatusFilter}
          />
          <span className={styles.countText}>전체 {events.length}건</span>
        </div>

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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}