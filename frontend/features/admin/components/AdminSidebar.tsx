"use client";

import Link from "next/link";
import { BarChart3, CalendarDays, ClipboardList, LogOut, Megaphone, PanelLeftClose, PanelLeftOpen, QrCode, LineChart, ScanLine, TicketCheck, Users } from "lucide-react";
import { usePathname } from "next/navigation";
import { USER_MENU_ITEMS } from "@/features/shell/constants/navigation";
import styles from "./AdminSidebar.module.css";

interface AdminSidebarProps {
  mode: "user" | "manager" | "admin";
  isOpen: boolean;
  onClose: () => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onLogout: () => void;
}

type NavLink = { type?: "link"; href: string; label: string; icon: React.ComponentType<{ className?: string }> };
type NavSection = { type: "section"; label: string };
type NavItem = NavLink | NavSection;

export function AdminSidebar({
  isOpen,
  onClose,
  isCollapsed,
  onToggleCollapse,
  onLogout,
  mode,
}: AdminSidebarProps) {
  const pathname = usePathname();
  const userIcons = {
    exhibitions: CalendarDays,
    reservations: ClipboardList,
    "qr-ticket": QrCode,
    payments: LineChart,
    "vip-ads": Megaphone,
  };
  const items: NavItem[] = mode === "user"
    ? USER_MENU_ITEMS.map((item) => ({ href: item.path, label: item.label, icon: userIcons[item.id] }))
    : mode === "manager" ? [
        { href: "/manager/events", label: "박람회 관리", icon: CalendarDays },
        { href: "/manager/reservations", label: "예약자 명단 관리", icon: ClipboardList },
        { href: "/manager/check-in", label: "QR 체크인", icon: ScanLine },
        { href: "/manager/check-in/status", label: "체크인 현황", icon: QrCode },
        { type: "section" as const, label: "마케팅" },
        { href: "/manager/banner-ads", label: "광고 관리", icon: Megaphone },
      ]
    : [
        { href: "/admin/events", label: "박람회 관리", icon: CalendarDays },
        { href: "/admin/reservations-summary", label: "행사별 예약 현황", icon: BarChart3 },
        { href: "/admin/members", label: "회원 관리", icon: Users },
        { href: "/admin/banners", label: "광고 관리", icon: Megaphone },
        { href: "/admin/payments", label: "매출 현황", icon: LineChart },
      ];
  return (
    <>
      {isOpen ? <div onClick={onClose} className={styles.backdrop} /> : null}
      <aside
        className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : styles.sidebarClosed}`}
        data-collapsed={isCollapsed}
      >
        <div className={styles.top}>
          <div className={styles.logoArea}>
            <button
              type="button"
              className={styles.logoGroup}
              onClick={() => window.location.reload()}
              aria-label="페이지 새로고침"
            >
              <div className={styles.logoBadge}><TicketCheck aria-hidden="true" /></div>
              <span className={styles.collapsible}>
                <span className={styles.logoText}>
                  <span className={styles.logoBrand}>EXPO PASS</span>
                  <span className={styles.logoSub}>{mode === "user" ? "User" : mode === "manager" ? "Manager" : "Admin"}</span>
                </span>
              </span>
            </button>
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
            {items.map((item) => {
              if (item.type === "section") {
                return (
                  <span key={item.label} className={`${styles.navSection} ${styles.collapsible}`}>
                    {item.label}
                  </span>
                );
              }
              const Icon = item.icon;
              return (
                <Link key={item.href} href={item.href} className={styles.navItem} data-active={pathname === item.href} onClick={onClose}>
                  <Icon className={styles.icon} />
                  <span className={styles.collapsible}>{item.label}</span>
                </Link>
              );
            })}
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
