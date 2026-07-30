"use client";

import { useGetEventSummaryQuery } from "../../api/adminApi";
import type { OrderStatus } from "../../types/reservation";
import styles from "./ReservationSummaryContent.module.css";

interface ReservationSummaryContentProps {
  eventId: string;
}

const STATUS_CONFIG: Record<OrderStatus, { label: string; color: "success" | "warning" | "danger" | "secondary" }> = {
  CONFIRMED: { label: "예약확정", color: "success" },
  PENDING: { label: "결제대기", color: "warning" },
  CANCELLED: { label: "취소됨", color: "danger" },
  REFUNDED: { label: "환불됨", color: "danger" },
};

export function ReservationSummaryContent({ eventId }: ReservationSummaryContentProps) {
  const { data: summary, isLoading } = useGetEventSummaryQuery(eventId);

  if (isLoading || !summary) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const statuses = Object.keys(STATUS_CONFIG) as OrderStatus[];
  const counts = statuses.map((status) => ({
    status,
    label: STATUS_CONFIG[status].label,
    color: STATUS_CONFIG[status].color,
    count: summary.countsByStatus[status] ?? 0,
  }));

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>예약 현황</h1>
        <p className={styles.subtitle}>총 예약 {summary.totalOrders}건</p>
      </header>

      <div className={styles.grid}>
        {counts.map((item) => (
          <div key={item.status} className={styles.card} data-color={item.color}>
            <p className={styles.label}>{item.label}</p>
            <p className={styles.count}>{item.count}</p>
            <p className={styles.percentage}>
              {summary.totalOrders === 0 ? "0" : ((item.count / summary.totalOrders) * 100).toFixed(1)}%
            </p>
          </div>
        ))}
      </div>

      <div className={styles.progressSection}>
        <h2 className={styles.sectionTitle}>진행률</h2>
        <div className={styles.progressCard}>
          <div className={styles.progressRow}>
            <span className={styles.progressLabel}>예약확정</span>
            <div className={styles.progressBar}>
              <div
                className={styles.progressFill}
                style={{
                  width: `${summary.totalOrders === 0 ? 0 : ((summary.countsByStatus.CONFIRMED ?? 0) / summary.totalOrders) * 100}%`,
                }}
              />
            </div>
            <span className={styles.progressValue}>{summary.countsByStatus.CONFIRMED ?? 0}</span>
          </div>
          <div className={styles.progressRow}>
            <span className={styles.progressLabel}>결제대기</span>
            <div className={styles.progressBar}>
              <div
                className={styles.progressFill}
                style={{
                  width: `${summary.totalOrders === 0 ? 0 : ((summary.countsByStatus.PENDING ?? 0) / summary.totalOrders) * 100}%`,
                }}
              />
            </div>
            <span className={styles.progressValue}>{summary.countsByStatus.PENDING ?? 0}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
