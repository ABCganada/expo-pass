"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CalendarDays, ClipboardList, ImageIcon, MoreVertical, ScanLine } from "lucide-react";
import { useGetCheckinProgressQuery } from "../../api/adminApi";
import type { AdminEventListItem } from "../../types/eventLookup";
import styles from "./CheckinStatusContent.module.css";

const PHASE_LABEL: Record<string, string> = {
  UPCOMING: "예정",
  ONGOING: "진행중",
  ENDED: "종료",
};

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
}

function progressLevel(percentage: number): "low" | "mid" | "high" {
  if (percentage >= 80) return "high";
  if (percentage >= 40) return "mid";
  return "low";
}

interface CheckinStatusCardProps {
  event: AdminEventListItem;
}

export function CheckinStatusCard({ event }: CheckinStatusCardProps) {
  const { data: progress, isLoading } = useGetCheckinProgressQuery(event.id);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  const total = progress?.totalItems ?? 0;
  const checkedIn = progress?.checkedInCount ?? 0;
  const percentage = total === 0 ? 0 : Math.round((checkedIn / total) * 100);

  return (
    <div className={styles.card}>
      <div className={styles.cardTop}>
        <div className={styles.avatar}>
          {event.thumbnailUrl ? (
            <img src={event.thumbnailUrl} alt="" className={styles.avatarImage} />
          ) : (
            <ImageIcon size={22} />
          )}
        </div>
        <div className={styles.cardHeaderText}>
          <div className={styles.badgeRow}>
            <span className={styles.category}>{event.categoryName}</span>
            <span className={styles.phase} data-phase={event.phase}>
              {PHASE_LABEL[event.phase] ?? event.phase}
            </span>
          </div>
          <h2 className={styles.cardTitle}>{event.title}</h2>
        </div>
        <div className={styles.menuWrapper} ref={menuRef}>
          <button
            type="button"
            className={styles.menuButton}
            aria-label="더보기"
            onClick={() => setMenuOpen((v) => !v)}
          >
            <MoreVertical size={18} />
          </button>
          {menuOpen && (
            <div className={styles.menu}>
              <Link
                href={`/manager/reservations/${event.id}`}
                className={styles.menuItem}
                onClick={() => setMenuOpen(false)}
              >
                <ClipboardList size={16} />
                예약자 명단 보기
              </Link>
              <Link
                href={`/manager/check-in/${event.id}`}
                className={styles.menuItem}
                onClick={() => setMenuOpen(false)}
              >
                <ScanLine size={16} />
                QR 체크인 하러가기
              </Link>
            </div>
          )}
        </div>
      </div>

      <div className={styles.cardMeta}>
        <CalendarDays size={14} />
        <span>
          {formatDate(event.startDate)} ~ {formatDate(event.endDate)}
        </span>
      </div>

      <div className={styles.progressSection}>
        <p className={styles.progressLabel}>입장 / 총 예약</p>
        {isLoading ? (
          <p className={styles.progressState}>불러오는 중...</p>
        ) : (
          <>
            <p className={styles.progressValue}>
              {checkedIn} / {total} <span className={styles.progressPercent}>({percentage}%)</span>
            </p>
            <div className={styles.progressBar}>
              <div
                className={styles.progressFill}
                data-level={progressLevel(percentage)}
                style={{ width: `${percentage}%` }}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
