"use client";

import Link from "next/link";
import { Building2, MapPin } from "lucide-react";
import type { EventListItem } from "../../types/event";
import { formatPeriod } from "../../utils/eventPhase";
import { EventPhaseBadge } from "../EventPhaseBadge";
import styles from "./EventCard.module.css";

interface EventCardProps {
  event: EventListItem;
}

export function EventCard({ event }: EventCardProps) {
  return (
    <Link href={`/exhibitions/${event.id}`} className={styles.card}>
      <div className={styles.thumbnail}>
        {event.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={event.thumbnailUrl} alt={event.title} className={styles.image} draggable={false} />
        ) : (
          <Building2 size={40} className={styles.placeholderIcon} />
        )}
        <EventPhaseBadge startDate={event.startDate} endDate={event.endDate} className={styles.ddayBadge} />
      </div>

      <div className={styles.body}>
        <div className={styles.venueRow}>
          <MapPin size={14} />
          <span className={styles.venue}>{event.venueName}</span>
        </div>
        <h3 className={styles.title}>{event.title}</h3>
        <p className={styles.period}>{formatPeriod(event.startDate, event.endDate)}</p>
      </div>
    </Link>
  );
}