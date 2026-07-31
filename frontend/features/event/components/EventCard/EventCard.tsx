"use client";

import { Building2, MapPin } from "lucide-react";
import type { EventListItem } from "../../types/event";
import { computeDdayLabel, computeEventPhase } from "../../utils/eventPhase";
import styles from "./EventCard.module.css";

interface EventCardProps {
  event: EventListItem;
  onClick?: (eventId: string) => void;
}

function formatPeriod(startDate: string, endDate: string): string {
  return `${startDate.replaceAll("-", ".")} ~ ${endDate.replaceAll("-", ".")}`;
}

export function EventCard({ event, onClick }: EventCardProps) {
  const phase = computeEventPhase(event.startDate, event.endDate);
  const ddayLabel = computeDdayLabel(event.startDate, event.endDate);

  const content = (
    <>
      <div className={styles.thumbnail}>
        {event.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={event.thumbnailUrl} alt={event.title} className={styles.image} draggable={false} />
        ) : (
          <Building2 size={40} className={styles.placeholderIcon} />
        )}
        <span className={styles.ddayBadge} data-phase={phase}>
          {ddayLabel}
        </span>
      </div>

      <div className={styles.body}>
        <div className={styles.venueRow}>
          <MapPin size={14} />
          <span className={styles.venue}>{event.venueName}</span>
        </div>
        <h3 className={styles.title}>{event.title}</h3>
        <p className={styles.period}>{formatPeriod(event.startDate, event.endDate)}</p>
      </div>
    </>
  );

  if (onClick) {
    return (
      <button type="button" className={styles.card} onClick={() => onClick(event.id)}>
        {content}
      </button>
    );
  }

  return <article className={styles.card}>{content}</article>;
}