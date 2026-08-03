"use client";

import { useState } from "react";
import { Filter, Search } from "lucide-react";
import { useGetAdminEventsForDisplayQuery } from "../../api/eventLookupApi";
import { CheckinStatusCard } from "./CheckinStatusCard";
import styles from "./CheckinStatusContent.module.css";

type PhaseFilter = "ALL" | "UPCOMING" | "ONGOING" | "ENDED";

const PHASE_FILTER_LABEL: Record<PhaseFilter, string> = {
  ALL: "전체",
  UPCOMING: "예정",
  ONGOING: "진행중",
  ENDED: "종료",
};

export function CheckinStatusContent() {
  const { data: events = [], isLoading } = useGetAdminEventsForDisplayQuery();
  const [phaseFilter, setPhaseFilter] = useState<PhaseFilter>("ALL");
  const [query, setQuery] = useState("");

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const phaseFiltered = phaseFilter === "ALL" ? events : events.filter((event) => event.phase === phaseFilter);
  const trimmedQuery = query.trim().toLowerCase();
  const filteredEvents = trimmedQuery
    ? phaseFiltered.filter((event) => event.title.toLowerCase().includes(trimmedQuery))
    : phaseFiltered;

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>체크인 현황</h1>
          <p className={styles.subtitle}>총 {events.length}개의 행사를 관리 중입니다.</p>
        </div>
        <div className={styles.headerControls}>
          <label className={styles.searchBar}>
            <Search size={16} aria-hidden />
            <input
              type="text"
              className={styles.searchInput}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="행사명으로 검색"
              aria-label="행사명 검색"
            />
          </label>
          <label className={styles.filterGroup}>
            <Filter size={16} />
            <select
              className={styles.filterSelect}
              value={phaseFilter}
              onChange={(e) => setPhaseFilter(e.target.value as PhaseFilter)}
              aria-label="행사 진행상태 필터"
            >
              {(Object.keys(PHASE_FILTER_LABEL) as PhaseFilter[]).map((key) => (
                <option key={key} value={key}>
                  {PHASE_FILTER_LABEL[key]}
                </option>
              ))}
            </select>
          </label>
        </div>
      </header>

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
