"use client";

import Link from "next/link";
import { Bookmark, CalendarDays, ClipboardList, HelpCircle, Home, Megaphone, QrCode, Receipt } from "lucide-react";
import { usePathname } from "next/navigation";
import { USER_MENU_ITEMS } from "../constants/navigation";
import styles from "./MobileBottomNav.module.css";

export function MobileBottomNav() {
  const pathname = usePathname();
  const icons = {
    home: Home,
    exhibitions: CalendarDays,
    reservations: ClipboardList,
    "qr-ticket": QrCode,
    bookmarks: Bookmark,
    payments: Receipt,
    "vip-ads": Megaphone,
    support: HelpCircle,
  };

  return (
    <nav className={styles.container} aria-label="하단 메뉴">
      {USER_MENU_ITEMS.map((item) => {
        const Icon = icons[item.id];
        return (
        <Link key={item.id} href={item.path} className={styles.navItem} data-active={pathname === item.path}>
          <Icon className={styles.icon} />
          <span className={styles.label}>{item.label}</span>
        </Link>
      );})}
    </nav>
  );
}
