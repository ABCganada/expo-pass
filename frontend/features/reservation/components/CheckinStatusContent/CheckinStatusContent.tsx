"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { useGetAdminEventsForDisplayQuery } from "../../api/eventLookupApi";
import { CheckinStatusCard } from "./CheckinStatusCard";
import styles from "./CheckinStatusContent.module.css";

type PhaseTabKey = "ALL" | "UPCOMING" | "ONGOING" | "ENDED";

const PHASE_TABS: { key: PhaseTabKey; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "UPCOMING", label: "예정" },
  { key: "ONGOING", label: "진행중" },
  { key: "ENDED", label: "종료" },
];

export function CheckinStatusContent() {
  const { data: rawEvents = [], isLoading } = useGetAdminEventsForDisplayQuery();
  // 예약이 있을 수 없는 DRAFT 행사는 체크인 현황에 보일 이유가 없으니 제외한다.
  const events = rawEvents.filter((event) => event.status !== "DRAFT");
  const [selectedTab, setSelectedTab] = useState<PhaseTabKey>("ALL");
  const [query, setQuery] = useState("");

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const phaseFiltered = selectedTab === "ALL" ? events : events.filter((event) => event.phase === selectedTab);
  const trimmedQuery = query.trim().toLowerCase();
  const filteredEvents = trimmedQuery
    ? phaseFiltered.filter((event) => event.title.toLowerCase().includes(trimmedQuery))
    : phaseFiltered;

  return (
    <div className={styles.page}>
      <div className={styles.topRow}>
        <div className={styles.filterTabs} role="tablist">
          {PHASE_TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={selectedTab === tab.key}
              className={styles.filterTab}
              data-active={selectedTab === tab.key}
              onClick={() => setSelectedTab(tab.key)}
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

      {filteredEvents.length === 0 ? (
        <div className={styles.state}>{trimmedQuery ? "검색 결과가 없습니다" : "표시할 행사가 없습니다"}</div>
      ) : (
        <div className={styles.grid}>
          {filteredEvents.map((event) => (
            <CheckinStatusCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  );
}
