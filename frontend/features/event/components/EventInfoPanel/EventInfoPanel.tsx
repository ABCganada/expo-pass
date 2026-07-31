"use client";

import { Bookmark, BookmarkCheck, Eye, MapPin } from "lucide-react";
import type { EventDetail } from "../../types/event";
import { formatPeriod } from "../../utils/eventPhase";
import { EventPhaseBadge } from "../EventPhaseBadge";
import styles from "./EventInfoPanel.module.css";

interface EventInfoPanelProps {
  detail: EventDetail;
  isBookmarked: boolean;
  isBookmarkPending: boolean;
  onToggleBookmark: () => void;
}

export function EventInfoPanel({ detail, isBookmarked, isBookmarkPending, onToggleBookmark }: EventInfoPanelProps) {
  const hasPeriod = !!detail.startDate && !!detail.endDate;

  return (
    <div className={styles.panel}>
      <div className={styles.topRow}>
        <span className={styles.categoryBadge}>{detail.categoryName}</span>
        <div className={styles.topRowRight}>
          <span className={styles.viewCount}>
            <Eye size={14} />
            조회수 {detail.viewCount.toLocaleString("ko-KR")}
          </span>
          <button
            type="button"
            className={styles.bookmarkButton}
            aria-label={isBookmarked ? "북마크 해제" : "북마크 추가"}
            aria-pressed={isBookmarked}
            disabled={isBookmarkPending}
            onClick={onToggleBookmark}
          >
            {isBookmarked ? <BookmarkCheck size={20} /> : <Bookmark size={20} />}
          </button>
        </div>
      </div>

      <h1 className={styles.title}>{detail.title}</h1>
      {detail.hostName && <p className={styles.hostName}>주최: {detail.hostName}</p>}

      <div className={styles.divider} />

      <div className={styles.row}>
        <span className={styles.rowLabel}>기간</span>
        <div className={styles.rowValueGroup}>
          <span className={styles.rowValue}>{hasPeriod ? formatPeriod(detail.startDate!, detail.endDate!) : "미정"}</span>
          {hasPeriod && <EventPhaseBadge startDate={detail.startDate!} endDate={detail.endDate!} />}
        </div>
      </div>

      <div className={styles.divider} />

      <div className={styles.row}>
        <span className={styles.rowLabel}>장소</span>
        <div className={styles.rowValueGroup}>
          <div className={styles.venueBlock}>
            <span className={styles.venueName}>
              <MapPin size={14} />
              {detail.venueName ?? "미정"}
            </span>
            {(detail.address || detail.detailAddress) && (
              <span className={styles.address}>
                {[detail.address, detail.detailAddress].filter(Boolean).join(" ")}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}