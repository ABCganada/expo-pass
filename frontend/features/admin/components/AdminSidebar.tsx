"use client";

import Link from "next/link";
import { BarChart3, Bookmark, CalendarDays, ClipboardList, Home, LineChart, LogOut, Megaphone, PanelLeftClose, PanelLeftOpen, QrCode, ScanLine, Users } from "lucide-react";
import { usePathname } from "next/navigation";
import { USER_MENU_ITEMS } from "@/features/shell/constants/navigation";
import { useAppSelector } from "@/features/store/hooks";
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
  // 라이트 배경엔 남색, 다크 배경엔 흰색 버전이 대비가 잘 나온다.
  const isDarkTheme = useAppSelector((state) => state.theme.name === "dark");
  const logoSrc = isDarkTheme ? "/logo-mark-white.png" : "/logo-mark-navy.png";
  const userIcons = {
    home: Home,
    exhibitions: CalendarDays,
    reservations: ClipboardList,
    "qr-ticket": QrCode,
    bookmarks: Bookmark,
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
        { href: "/manager/settlements", label: "정산 현황", icon: LineChart },
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
  // 상세 페이지(/manager/reservations/21 등)도 목록 메뉴가 계속 강조되도록 prefix로 판단한다.
  // /manager/check-in과 /manager/check-in/status처럼 겹치는 경로가 있어 가장 길게 일치하는 것만 켠다.
  const activeHref = items
    .filter((item): item is NavLink => item.type !== "section")
    .map((item) => item.href)
    .filter((href) => pathname === href || pathname.startsWith(`${href}/`))
    .sort((a, b) => b.length - a.length)[0];
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
              <div className={styles.logoBadge}>
                <img src={logoSrc} alt="" className={styles.logoImage} />
              </div>
              <span className={styles.collapsible}>
                <span className={styles.logoBrand}>
                  <span className={styles.logoBrandAccent}>EXPO</span> PASS
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
                <Link key={item.href} href={item.href} className={styles.navItem} data-active={item.href === activeHref} onClick={onClose}>
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
