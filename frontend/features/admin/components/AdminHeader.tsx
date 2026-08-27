import React from "react";
import styles from "./AdminHeader.module.css";
import { CurrentUser } from "../../auth/types/auth";
import { ModeSwitcher, type AppMode } from "../../auth/components/ModeSwitcher/ModeSwitcher";
import { ThemeToggle } from "../../theme/components/ThemeToggle/ThemeToggle";

interface AdminHeaderProps {
  onMenuToggle: () => void;
  user: CurrentUser | null;
  onLogout: () => void;
  activeMode: AppMode;
}

export function AdminHeader({ onMenuToggle, user, onLogout, activeMode }: AdminHeaderProps) {
  const displayName = user?.name?.trim() || user?.email?.trim() || null;
  const modeTitle =
    activeMode === "user" ? "박람회 예약"
    : activeMode === "manager" ? "박람회 운영"
    : activeMode === "developer" ? "개발자 모니터링"
    : "운영 대시보드";

  return (
    <header className={styles.header}>
      {/* 좌측 영역 */}
      <div className={styles.leftSection}>
        {/* 햄버거 메뉴 토글 버튼 (모바일 전용) */}
        <button
          type="button"
          onClick={onMenuToggle}
          aria-label="메뉴 토글"
          className={styles.menuToggle}
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className={styles.menuIcon}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
          </svg>
        </button>

        <div className={styles.titleArea}>
          <h2 className={styles.title}>
            {modeTitle}
          </h2>
        </div>
      </div>

      {/* 우측 영역 */}
      <div className={styles.rightSection}>
        <ModeSwitcher activeMode={activeMode} roles={user?.roles ?? []} compact />
        <div className={styles.userGroup}>
          {displayName ? (
            <div className={styles.userArea}>
              <div className={styles.userAvatar}>
                {displayName[0]}
              </div>
              <span className={styles.userName}>
                {displayName}님
              </span>
            </div>
          ) : null}
          <button
            onClick={onLogout}
            className={styles.logoutButton}
          >
            로그아웃
          </button>
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
