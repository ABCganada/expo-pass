"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ClipboardList, Search } from "lucide-react";
import { ReservationListItem } from "../ReservationListItem";
import { useGetMyOrdersQuery } from "../../api/reservationApi";
import { useLazyGetEventForDisplayQuery } from "../../api/eventLookupApi";
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
  const [query, setQuery] = useState("");
  const [titlesByEventId, setTitlesByEventId] = useState<Record<string, string>>({});
  const [triggerGetEvent] = useLazyGetEventForDisplayQuery();
  const router = useRouter();

  // 검색은 행사명 기준인데 OrderSummary엔 행사명이 없다 — eventId별로 한 번만 조회해서 채워둔다
  // (ReservationListItem도 같은 쿼리를 쓰므로 RTK Query 캐시가 중복 요청을 걸러준다).
  useEffect(() => {
    const uniqueEventIds = Array.from(new Set(orders.map((order) => order.eventId)));
    uniqueEventIds
      .filter((eventId) => !(eventId in titlesByEventId))
      .forEach((eventId) => {
        triggerGetEvent(eventId)
          .unwrap()
          .then((event) => setTitlesByEventId((prev) => ({ ...prev, [eventId]: event.title })))
          .catch(() => {});
      });
  }, [orders, titlesByEventId, triggerGetEvent]);

  const trimmedQuery = query.trim().toLowerCase();
  const filteredOrders = orders.filter((order) => {
    if (filter !== "ALL" && order.status !== filter) return false;
    if (trimmedQuery) {
      const title = titlesByEventId[order.eventId];
      if (!title || !title.toLowerCase().includes(trimmedQuery)) return false;
    }
    return true;
  });

  const handleViewEvent = (eventId: string) => {
    router.push(`/exhibitions/${eventId}`);
  };

  return (
    <div className={styles.page}>
      <div className={styles.topRow}>
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

        <div className={styles.searchBar}>
          <Search size={16} aria-hidden />
          <input
            type="text"
            className={styles.searchInput}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="행사명으로 검색"
            aria-label="행사명 검색"
          />
        </div>
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
            <p className={styles.emptyTitle}>
              {trimmedQuery ? "검색 결과가 없습니다" : "해당하는 예약 내역이 없습니다"}
            </p>
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
