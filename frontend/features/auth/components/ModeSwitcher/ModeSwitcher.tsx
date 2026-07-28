import Link from "next/link";
import styles from "./ModeSwitcher.module.css";

export type AppMode = "user" | "manager" | "admin";

interface ModeSwitcherProps {
  activeMode: AppMode;
  roles: string[];
  compact?: boolean;
}

interface RoleTab {
  role: string;
  mode: AppMode;
  label: string;
  href: string;
}

/**
 * 권한 → 탭 정의. 배열 순서가 곧 탭이 보이는 좌→우 순서다.
 * 새 권한에 화면을 붙이려면 여기에 한 줄만 추가하면 스위처에 자동 반영된다.
 */
const ROLE_TABS: RoleTab[] = [
  { role: "USER", mode: "user", label: "일반 유저", href: "/exhibitions" },
  { role: "MANAGER", mode: "manager", label: "박람회 관리자", href: "/manager/reservations" },
  { role: "ADMIN", mode: "admin", label: "전체 관리자", href: "/admin/exhibitions" },
];

export function ModeSwitcher({
  activeMode,
  roles,
  compact = false,
}: ModeSwitcherProps) {
  const tabs = ROLE_TABS.filter((tab) => roles.includes(tab.role));

  // 전환할 곳이 없으면(예: USER 권한만 보유) 스위처를 아예 숨긴다.
  if (tabs.length < 2) return null;

  return (
    <nav aria-label="화면 모드 전환" className={styles.switcher} data-compact={compact}>
      {tabs.map(({ mode, label, href }) => (
        <Link
          key={mode}
          href={href}
          aria-current={activeMode === mode ? "page" : undefined}
          className={styles.link}
        >
          {label}
        </Link>
      ))}
    </nav>
  );
}
