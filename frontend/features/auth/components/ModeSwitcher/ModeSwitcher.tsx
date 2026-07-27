import Link from "next/link";
import styles from "./ModeSwitcher.module.css";

type AppMode = "user" | "admin";

interface ModeSwitcherProps {
  activeMode: AppMode;
  roles: string[];
  compact?: boolean;
}

const modes: Array<{ mode: AppMode; label: string; href: string }> = [
  { mode: "user", label: "유저 모드", href: "/" },
  { mode: "admin", label: "관리자 모드", href: "/admin" },
];

export function ModeSwitcher({
  activeMode,
  roles,
  compact = false,
}: ModeSwitcherProps) {
  const roleCount = new Set(roles.filter(Boolean)).size;

  if (roleCount < 2) {
    return null;
  }

  return (
    <nav aria-label="화면 모드 전환" className={styles.switcher} data-compact={compact}>
      {modes.map(({ mode, label, href }) => (
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
