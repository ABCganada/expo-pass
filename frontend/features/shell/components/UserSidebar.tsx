"use client";

import Link from "next/link";
import { CalendarDays, ClipboardList, Home, LogOut, Megaphone, PanelLeftClose, PanelLeftOpen, QrCode, Receipt } from "lucide-react";
import { usePathname } from "next/navigation";
import { USER_MENU_ITEMS } from "../constants/navigation";
import styles from "./UserSidebar.module.css";

interface UserSidebarProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onLogout: () => void;
}

export function UserSidebar({ isCollapsed, onToggleCollapse, onLogout }: UserSidebarProps) {
  const pathname = usePathname();
  const icons = {
    home: Home,
    exhibitions: CalendarDays,
    reservations: ClipboardList,
    "qr-ticket": QrCode,
    payments: Receipt,
    "vip-ads": Megaphone,
  };

  return (
    <aside className={styles.sidebar} data-collapsed={isCollapsed}>
      <div className={styles.top}>
        <div className={styles.logoArea}>
          <div className={styles.logoGroup}>
            <div className={styles.logoBadge}>L</div>
            <span className={styles.collapsible}>
              <span className={styles.logoBrand}>Last Mission</span>
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
          {USER_MENU_ITEMS.map((item) => {
            const Icon = icons[item.id];
            return (
            <Link key={item.id} href={item.path} className={styles.navLink} data-active={pathname === item.path}>
              <Icon className={styles.navIcon} />
              <span className={styles.collapsible}>{item.label}</span>
            </Link>
          );})}
        </nav>
      </div>

      <div className={styles.bottom}>
        <button onClick={onLogout} className={styles.logoutButton}>
          <LogOut className={styles.logoutIcon} />
          <span className={styles.collapsible}>로그아웃</span>
        </button>
      </div>
    </aside>
  );
}
