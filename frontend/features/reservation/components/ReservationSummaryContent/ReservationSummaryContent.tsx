"use client";

import { useGetDailyReservationCountsForAdminQuery, useGetEventSummaryForAdminQuery } from "../../api/adminApi";
import type { OrderStatus } from "../../types/reservation";
import { DailyReservationChart } from "./DailyReservationChart";
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
  const { data: summary, isLoading } = useGetEventSummaryForAdminQuery(eventId);
  const { data: dailyCounts = [] } = useGetDailyReservationCountsForAdminQuery(eventId);

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

      <DailyReservationChart data={dailyCounts} />
    </div>
  );
}
