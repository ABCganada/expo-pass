"use client";

import { useState } from "react";
import { Filter } from "lucide-react";
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

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const filteredEvents = phaseFilter === "ALL" ? events : events.filter((event) => event.phase === phaseFilter);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>체크인 현황</h1>
          <p className={styles.subtitle}>총 {events.length}개의 행사를 관리 중입니다.</p>
        </div>
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
      </header>

      {filteredEvents.length === 0 ? (
        <div className={styles.state}>표시할 행사가 없습니다</div>
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
