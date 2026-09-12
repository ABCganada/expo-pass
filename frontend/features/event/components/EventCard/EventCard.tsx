"use client";

import Link from "next/link";
import { Bookmark, Building2, MapPin } from "lucide-react";
import type { EventListItem } from "../../types/event";
import { formatPeriod } from "../../utils/eventPhase";
import { EventPhaseBadge } from "../EventPhaseBadge";
import styles from "./EventCard.module.css";

interface EventCardProps {
  event: EventListItem;
  isBookmarked?: boolean;
  onToggleBookmark?: (eventId: string) => void;
}

export function EventCard({ event, isBookmarked = false, onToggleBookmark }: EventCardProps) {
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
        {onToggleBookmark && (
          <button
            type="button"
            className={styles.bookmarkButton}
            aria-label={isBookmarked ? "북마크 해제" : "북마크 추가"}
            aria-pressed={isBookmarked}
            onClick={(event_) => {
              event_.preventDefault();
              event_.stopPropagation();
              onToggleBookmark(event.id);
            }}
          >
            <Bookmark size={20} fill={isBookmarked ? "currentColor" : "none"} />
          </button>
        )}
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