"use client";

import { CalendarDays } from "lucide-react";
import { useGetEventsQuery } from "../../api/eventApi";
import { EventCard } from "../EventCard/EventCard";
import styles from "./HomeEventSection.module.css";

const DISPLAY_LIMIT = 8;

export function HomeEventSection() {
  const { data: events = [], isLoading } = useGetEventsQuery(undefined);
  const displayedEvents = events.slice(0, DISPLAY_LIMIT);

  return (
    <section className={styles.section}>
      <h2 className={styles.title}>지금 주목할 만한 박람회</h2>

      {isLoading && (
        <div className={styles.grid}>
          {Array.from({ length: DISPLAY_LIMIT }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && displayedEvents.length === 0 && (
        <div className={styles.empty}>
          <CalendarDays size={48} className={styles.emptyIcon} />
          <p className={styles.emptyText}>진행 중인 박람회가 없습니다.</p>
        </div>
      )}

      {!isLoading && displayedEvents.length > 0 && (
        <div className={styles.grid}>
          {displayedEvents.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </section>
  );
}