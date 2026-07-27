"use client";

import Link from "next/link";
import { Home } from "lucide-react";
import { USER_MENU_ITEMS } from "../constants/navigation";
import styles from "./MobileBottomNav.module.css";

export function MobileBottomNav() {
  return (
    <nav className={styles.container} aria-label="하단 메뉴">
      {USER_MENU_ITEMS.map((item) => (
        <Link key={item.id} href={item.path} className={styles.navItem} data-active="true">
          <Home className={styles.icon} />
          <span className={styles.label}>{item.label}</span>
        </Link>
      ))}
    </nav>
  );
}
