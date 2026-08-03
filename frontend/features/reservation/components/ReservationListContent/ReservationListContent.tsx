"use client";

import { useEffect, useMemo, useState } from "react";
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

const PAGE_SIZE = 10;
// 검색 중엔 서버 페이징 대신, 검색어와 맞는 걸 다 찾을 수 있도록 한 번에 넉넉히 받아와서
// 클라이언트에서 행사명으로 거른다(행사명은 이 목록 API엔 없어서 서버 검색은 지원하지 않는다).
const SEARCH_BATCH_SIZE = 200;

export function ReservationListContent() {
  const [filter, setFilter] = useState<FilterKey>("ALL");
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [titlesByEventId, setTitlesByEventId] = useState<Record<string, string>>({});
  const [triggerGetEvent] = useLazyGetEventForDisplayQuery();
  const router = useRouter();

  // 타이핑할 때마다 200건짜리 검색 배치를 다시 훑지 않도록 살짝 늦춰서 반영한다.
  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), query ? 250 : 0);
    return () => window.clearTimeout(timer);
  }, [query]);

  const trimmedQuery = debouncedQuery.trim().toLowerCase();
  const isSearching = trimmedQuery.length > 0;

  const { data, isLoading: isOrdersLoading, isFetching } = useGetMyOrdersQuery({
    status: filter === "ALL" ? undefined : filter,
    page: isSearching ? 0 : page,
    size: isSearching ? SEARCH_BATCH_SIZE : PAGE_SIZE,
  });
  // 디바운스 대기 중엔 아직 최신 검색어 기준 결과가 아니므로, "검색 결과 없음"이 잠깐
  // 스쳐 지나가지 않도록 로딩으로 취급한다.
  const isLoading = isOrdersLoading || query !== debouncedQuery;
  const orders = useMemo(() => data?.orders ?? [], [data]);
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handleFilterChange = (key: FilterKey) => {
    setFilter(key);
    // 이전 필터 기준으로 보던 페이지 번호는 더 이상 의미가 없으니 첫 페이지로 되돌린다.
    setPage(0);
  };

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

  const filteredOrders = isSearching
    ? orders.filter((order) => {
        const title = titlesByEventId[order.eventId];
        return !!title && title.toLowerCase().includes(trimmedQuery);
      })
    : orders;

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
              onClick={() => handleFilterChange(tab.key)}
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
              {isSearching ? "검색 결과가 없습니다" : "해당하는 예약 내역이 없습니다"}
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

      {!isSearching && totalPages > 1 ? (
        <footer className={styles.pagination}>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page <= 0 || isFetching}
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
          >
            이전
          </button>
          <span className={styles.pageInfo}>
            {page + 1} / {totalPages} (총 {totalElements}건)
          </span>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page + 1 >= totalPages || isFetching}
            onClick={() => setPage((value) => value + 1)}
          >
            다음
          </button>
        </footer>
      ) : null}
    </div>
  );
}
