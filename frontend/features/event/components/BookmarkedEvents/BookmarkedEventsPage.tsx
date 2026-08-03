"use client";

import { useState } from "react";
import { CalendarDays } from "lucide-react";
import { useGetMyBookmarksQuery, useToggleBookmarkMutation } from "../../api/eventApi";
import { EventCard } from "../EventCard/EventCard";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import styles from "./BookmarkedEventsPage.module.css";

type PhaseFilter = "ALL" | "UPCOMING" | "ONGOING" | "ENDED";

const PHASE_OPTIONS: { value: PhaseFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "UPCOMING", label: "시작 전" },
  { value: "ONGOING", label: "진행 중" },
  { value: "ENDED", label: "종료" },
];

export function BookmarkedEventsPage() {
  const [phaseFilter, setPhaseFilter] = useState<PhaseFilter>("ALL");
  const { data: bookmarks = [], isLoading } = useGetMyBookmarksQuery();
  const [toggleBookmark] = useToggleBookmarkMutation();

  const filteredBookmarks =
    phaseFilter === "ALL" ? bookmarks : bookmarks.filter((bookmark) => bookmark.phase === phaseFilter);

  return (
    <div className={styles.page}>
      <div className={styles.filterRow}>
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

      {!isLoading && filteredBookmarks.length === 0 && (
        <div className={styles.empty}>
          <CalendarDays size={48} className={styles.emptyIcon} />
          <p className={styles.emptyText}>북마크한 박람회가 없습니다.</p>
        </div>
      )}

      {!isLoading && filteredBookmarks.length > 0 && (
        <div className={styles.grid}>
          {filteredBookmarks.map((bookmark) => (
            <EventCard
              key={bookmark.id}
              event={{ ...bookmark, viewCount: 0 }}
              isBookmarked
              onToggleBookmark={(eventId) => void toggleBookmark(eventId)}
            />
          ))}
        </div>
      )}
    </div>
  );
}