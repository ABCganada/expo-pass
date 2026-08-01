"use client";

import Link from "next/link";
import { CalendarDays, ChevronRight, ImageIcon } from "lucide-react";
import { useGetAdminEventsForDisplayQuery } from "../../api/eventLookupApi";
import styles from "./EventSelectContent.module.css";

interface EventSelectContentProps {
  basePath: string;
  title: string;
  subtitle: string;
}

const PHASE_LABEL: Record<string, string> = {
  UPCOMING: "예정",
  ONGOING: "진행중",
  ENDED: "종료",
};

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
}

export function EventSelectContent({ basePath, title, subtitle }: EventSelectContentProps) {
  const { data: events = [], isLoading } = useGetAdminEventsForDisplayQuery();

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  if (events.length === 0) {
    return (
      <div className={styles.page}>
        <header className={styles.header}>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.subtitle}>{subtitle}</p>
        </header>
        <div className={styles.state}>관리 중인 행사가 없습니다</div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        <p className={styles.subtitle}>{subtitle}</p>
      </header>

      <div className={styles.grid}>
        {events.map((event) => (
          <Link key={event.id} href={`${basePath}/${event.id}`} className={styles.card}>
            <div className={styles.thumbnail}>
              {event.thumbnailUrl ? (
                <img src={event.thumbnailUrl} alt="" className={styles.thumbnailImage} />
              ) : (
                <ImageIcon size={32} />
              )}
            </div>
            <div className={styles.cardHeader}>
              <span className={styles.category}>{event.categoryName}</span>
              <span className={styles.phase} data-phase={event.phase}>
                {PHASE_LABEL[event.phase] ?? event.phase}
              </span>
            </div>
            <h2 className={styles.cardTitle}>{event.title}</h2>
            <div className={styles.cardMeta}>
              <CalendarDays size={14} />
              <span>
                {formatDate(event.startDate)} ~ {formatDate(event.endDate)}
              </span>
            </div>
            <ChevronRight className={styles.arrow} size={20} />
          </Link>
        ))}
      </div>
    </div>
  );
}
