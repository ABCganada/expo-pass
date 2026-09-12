"use client";

import { useState } from "react";
import Link from "next/link";
import { CalendarDays, ChevronRight, ImageIcon, Search } from "lucide-react";
import { useGetAdminEventsForDisplayQuery } from "../../api/eventLookupApi";
import styles from "./EventSelectContent.module.css";

interface EventSelectContentProps {
  basePath: string;
  title: string;
  subtitle: string;
  /** 지정하면 이 phase(예: "ONGOING")의 행사만 보여준다. showPhaseTabs와는 같이 쓰지 않는다. */
  phaseFilter?: string;
  /** true면 전체/예정/진행중/종료 탭을 보여주고, 사용자가 직접 골라서 필터링한다. */
  showPhaseTabs?: boolean;
  /** true면 행사명으로 검색할 수 있는 입력창을 보여준다. */
  showSearch?: boolean;
}

type PhaseTabKey = "ALL" | "UPCOMING" | "ONGOING" | "ENDED";

const PHASE_LABEL: Record<string, string> = {
  UPCOMING: "예정",
  ONGOING: "진행중",
  ENDED: "종료",
};

const PHASE_TABS: { key: PhaseTabKey; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "UPCOMING", label: "예정" },
  { key: "ONGOING", label: "진행중" },
  { key: "ENDED", label: "종료" },
];

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
}

export function EventSelectContent({ basePath, phaseFilter, showPhaseTabs, showSearch }: EventSelectContentProps) {
  const { data: allEvents = [], isLoading } = useGetAdminEventsForDisplayQuery();
  const [selectedTab, setSelectedTab] = useState<PhaseTabKey>("ALL");
  const [query, setQuery] = useState("");

  const effectivePhaseFilter = showPhaseTabs ? (selectedTab === "ALL" ? undefined : selectedTab) : phaseFilter;
  const phaseFiltered = effectivePhaseFilter
    ? allEvents.filter((event) => event.phase === effectivePhaseFilter)
    : allEvents;
  const trimmedQuery = query.trim().toLowerCase();
  const events = trimmedQuery
    ? phaseFiltered.filter((event) => event.title.toLowerCase().includes(trimmedQuery))
    : phaseFiltered;

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  if (allEvents.length === 0) {
    return (
      <div className={styles.page}>
        <div className={styles.state}>관리 중인 행사가 없습니다</div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      {(showPhaseTabs || showSearch) && (
        <div className={styles.topRow}>
          {showPhaseTabs && (
            <div className={styles.filterTabs} role="tablist">
              {PHASE_TABS.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  role="tab"
                  aria-selected={selectedTab === tab.key}
                  className={styles.filterTab}
                  data-active={selectedTab === tab.key}
                  onClick={() => setSelectedTab(tab.key)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          {showSearch && (
            <div className={styles.searchBar}>
              <Search size={16} aria-hidden />
              <input
                type="text"
                className={styles.searchInput}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="행사명으로 검색"
                aria-label="행사명 검색"
              />
            </div>
          )}
        </div>
      )}

      {events.length === 0 ? (
        <div className={styles.state}>
          {trimmedQuery
            ? "검색 결과가 없습니다"
            : effectivePhaseFilter
              ? `${PHASE_LABEL[effectivePhaseFilter] ?? effectivePhaseFilter} 행사가 없습니다`
              : "관리 중인 행사가 없습니다"}
        </div>
      ) : (
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
      )}
    </div>
  );
}
