"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { CalendarDays } from "lucide-react";
import { useGetEventCategoriesQuery, useGetEventsQuery } from "../../api/eventApi";
import { CategoryTabs } from "../CategoryTabs/CategoryTabs";
import { EventCard } from "../EventCard/EventCard";
import styles from "./EventListContent.module.css";

export function EventListContent() {
  const searchParams = useSearchParams();
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(() => searchParams.get("category"));
  const { data: categories = [] } = useGetEventCategoriesQuery();
  const { data: events = [], isLoading, isFetching } = useGetEventsQuery(activeCategoryId ?? undefined);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>박람회</h1>
        <p className={styles.subtitle}>진행 중이거나 예정된 박람회를 둘러보세요.</p>
      </header>

      <CategoryTabs
        categories={categories}
        activeCategoryId={activeCategoryId}
        onSelect={setActiveCategoryId}
      />

      {isLoading && (
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && events.length === 0 && (
        <div className={styles.empty}>
          <CalendarDays size={48} className={styles.emptyIcon} />
          <p className={styles.emptyText}>해당하는 박람회가 없습니다.</p>
        </div>
      )}

      {!isLoading && events.length > 0 && (
        <div className={styles.grid} data-fetching={isFetching} aria-busy={isFetching}>
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  );
}