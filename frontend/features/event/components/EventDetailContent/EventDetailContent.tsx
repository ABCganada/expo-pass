"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  useGetEventDetailQuery,
  useGetMyBookmarksQuery,
  useToggleBookmarkMutation,
} from "../../api/eventApi";
import { EventGallery } from "../EventGallery";
import { EventInfoPanel } from "../EventInfoPanel";
import { EventTicketSection } from "../EventTicketSection";
import { EventDescriptionSection } from "../EventDescriptionSection";
import { EventLocationSection } from "../EventLocationSection";
import styles from "./EventDetailContent.module.css";

export function EventDetailContent() {
  const { eventId } = useParams<{ eventId: string }>();

  const { data: detail, isLoading, error } = useGetEventDetailQuery(eventId);
  const { data: bookmarks = [] } = useGetMyBookmarksQuery();
  const [toggleBookmark, { isLoading: isTogglingBookmark }] = useToggleBookmarkMutation();

  const isBookmarked = bookmarks.some((bookmark) => bookmark.id === eventId);

  if (isLoading) {
    return (
      <div className={styles.page}>
        <div className={styles.skeleton} aria-busy="true" />
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className={styles.page}>
        <p className={styles.errorText}>행사를 찾을 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <Link href="/exhibitions" className={styles.backLink}>
        <ArrowLeft size={16} />
        목록으로
      </Link>

      <div className={styles.mainGrid}>
        <EventGallery images={detail.images} title={detail.title} />

        <div className={styles.infoColumn}>
          <EventInfoPanel
            detail={detail}
            isBookmarked={isBookmarked}
            isBookmarkPending={isTogglingBookmark}
            onToggleBookmark={() => toggleBookmark(eventId)}
          />
          <EventTicketSection eventId={eventId} tickets={detail.tickets} />
        </div>
      </div>

      <EventDescriptionSection contents={detail.contents} />
      <EventLocationSection detail={detail} />
    </div>
  );
}