"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { CalendarDays } from "lucide-react";
import { useGetEventCategoriesQuery, useGetEventsQuery } from "../../api/eventApi";
import { CategoryTabs } from "../CategoryTabs/CategoryTabs";
import { EventCard } from "../EventCard/EventCard";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import styles from "./EventListContent.module.css";

type PhaseFilter = "ALL" | "UPCOMING" | "ONGOING" | "ENDED";

const PHASE_OPTIONS: { value: PhaseFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "UPCOMING", label: "시작 전" },
  { value: "ONGOING", label: "진행 중" },
  { value: "ENDED", label: "종료" },
];

export function EventListContent() {
  const searchParams = useSearchParams();
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(() => searchParams.get("category"));
  const [phaseFilter, setPhaseFilter] = useState<PhaseFilter>("ALL");
  const { data: categories = [] } = useGetEventCategoriesQuery();
  const { data: events = [], isLoading, isFetching } = useGetEventsQuery(activeCategoryId ?? undefined);

  const filteredEvents = phaseFilter === "ALL" ? events : events.filter((event) => event.phase === phaseFilter);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>박람회</h1>
        <p className={styles.subtitle}>진행 중이거나 예정된 박람회를 둘러보세요.</p>
      </header>

      <div className={styles.filterRow}>
        <div className={styles.categoryTabsWrap}>
          <CategoryTabs
            categories={categories}
            activeCategoryId={activeCategoryId}
            onSelect={setActiveCategoryId}
          />
        </div>
        <StatusFilterDropdown
          label="상태"
          options={PHASE_OPTIONS}
          value={phaseFilter}
          onChange={setPhaseFilter}
        />
      </div>

      {isLoading && (
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && filteredEvents.length === 0 && (
        <div className={styles.empty}>
          <CalendarDays size={48} className={styles.emptyIcon} />
          <p className={styles.emptyText}>해당하는 박람회가 없습니다.</p>
        </div>
      )}

      {!isLoading && filteredEvents.length > 0 && (
        <div className={styles.grid} data-fetching={isFetching} aria-busy={isFetching}>
          {filteredEvents.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  );
}