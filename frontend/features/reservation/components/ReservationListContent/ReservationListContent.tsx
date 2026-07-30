"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ClipboardList } from "lucide-react";
import { ReservationListItem } from "../ReservationListItem";
import { useGetMyOrdersQuery } from "../../api/reservationApi";
import type { OrderStatus } from "../../types/reservation";
import styles from "./ReservationListContent.module.css";

type FilterKey = "ALL" | OrderStatus;

const FILTER_TABS: { key: FilterKey; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "CONFIRMED", label: "예약확정" },
  { key: "PENDING", label: "결제대기" },
  { key: "CANCELLED", label: "취소됨" },
];

export function ReservationListContent() {
  const { data: orders = [], isLoading } = useGetMyOrdersQuery();
  const [filter, setFilter] = useState<FilterKey>("ALL");
  const router = useRouter();

  const filteredOrders =
    filter === "ALL"
      ? orders
      : orders.filter((order) => order.status === filter);

  // TODO: Event 팀이 실제 행사 상세 라우트(예: /exhibitions/[eventId])를 만들면 그쪽으로 교체
  const handleViewEvent = (eventId: string) => {
    router.push(`/exhibitions?eventId=${eventId}`);
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <p className={styles.subtitle}>
          예약한 박람회 티켓의 상태를 확인하고 관리할 수 있습니다.
        </p>
      </header>

      <div className={styles.filterTabs} role="tablist">
        {FILTER_TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={filter === tab.key}
            className={styles.filterTab}
            data-active={filter === tab.key}
            onClick={() => setFilter(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && (
        <div className={styles.list}>
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && filteredOrders.length === 0 && (
        <div className={styles.emptyState}>
          <div className={styles.emptyIconWrap}>
            <ClipboardList size={28} />
          </div>
          <div className={styles.emptyBody}>
            <p className={styles.emptyTitle}>해당하는 예약 내역이 없습니다</p>
            <p className={styles.emptyDescription}>
              박람회를 둘러보고 티켓을 예약해보세요.
            </p>
          </div>
        </div>
      )}

      {!isLoading && filteredOrders.length > 0 && (
        <div className={styles.list}>
          {filteredOrders.map((order) => (
            <ReservationListItem key={order.orderId} order={order} onViewEvent={handleViewEvent} />
          ))}
        </div>
      )}
    </div>
  );
}
