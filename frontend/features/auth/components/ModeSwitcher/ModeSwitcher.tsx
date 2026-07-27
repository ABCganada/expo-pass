import Link from "next/link";
import styles from "./ModeSwitcher.module.css";

export type AppMode = "user" | "manager" | "admin";

interface ModeSwitcherProps {
  activeMode: AppMode;
  roles: string[];
  compact?: boolean;
}

const modes: Array<{ mode: AppMode; label: string; href: string }> = [
  { mode: "user", label: "일반 유저", href: "/exhibitions" },
  { mode: "manager", label: "박람회 관리자", href: "/manager/reservations" },
  { mode: "admin", label: "전체 관리자", href: "/admin/exhibitions" },
];

function canAccess(mode: AppMode, roles: string[]) {
  if (mode === "user") return roles.length > 0;
  if (mode === "manager") return roles.includes("MANAGER") || roles.includes("ADMIN");
  return roles.includes("ADMIN");
}

export function ModeSwitcher({
  activeMode,
  roles,
  compact = false,
}: ModeSwitcherProps) {
  return (
    <nav aria-label="화면 모드 전환" className={styles.switcher} data-compact={compact}>
      {modes.map(({ mode, label, href }) => canAccess(mode, roles) ? (
          <Link
            key={mode}
            href={href}
            aria-current={activeMode === mode ? "page" : undefined}
            className={styles.link}
          >
            {label}
          </Link>
        ) : (
          <span key={mode} className={styles.link} aria-disabled="true" title="권한이 필요합니다">
            {label}
          </span>
        ))}
    </nav>
  );
}
