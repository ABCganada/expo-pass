import Link from "next/link";
import { LogOut, PanelLeftClose, PanelLeftOpen, Users } from "lucide-react";
import styles from "./AdminSidebar.module.css";

interface AdminSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onLogout: () => void;
}

export function AdminSidebar({
  isOpen,
  onClose,
  isCollapsed,
  onToggleCollapse,
  onLogout,
}: AdminSidebarProps) {
  return (
    <>
      {isOpen ? <div onClick={onClose} className={styles.backdrop} /> : null}
      <aside
        className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : styles.sidebarClosed}`}
        data-collapsed={isCollapsed}
      >
        <div className={styles.top}>
          <div className={styles.logoArea}>
            <div className={styles.logoGroup}>
              <div className={styles.logoBadge}>L</div>
              <span className={styles.collapsible}>
                <span className={styles.logoBrand}>Last Mission</span>
                <span className={styles.logoSub}>Admin</span>
              </span>
            </div>
            <button
              type="button"
              onClick={onToggleCollapse}
              aria-label={isCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
              className={styles.toggleButton}
            >
              {isCollapsed ? <PanelLeftOpen className={styles.toggleIcon} /> : <PanelLeftClose className={styles.toggleIcon} />}
            </button>
          </div>
          <nav className={styles.nav}>
            <Link href="/admin/members" className={styles.navItem} data-active="true" onClick={onClose}>
              <Users className={styles.icon} />
              <span className={styles.collapsible}>회원 관리</span>
            </Link>
          </nav>
        </div>
        <div className={styles.bottom}>
          <button onClick={onLogout} className={styles.logoutButton}>
            <LogOut className={styles.icon} />
            <span className={styles.collapsible}>로그아웃</span>
          </button>
        </div>
      </aside>
    </>
  );
}
