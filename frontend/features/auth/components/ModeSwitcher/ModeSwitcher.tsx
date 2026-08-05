import Link from "next/link";
import styles from "./ModeSwitcher.module.css";

export type AppMode = "user" | "manager" | "admin" | "developer";

interface ModeSwitcherProps {
  activeMode: AppMode;
  roles: string[];
  compact?: boolean;
}

interface RoleTab {
  role: string;
  label: string;
  /** 연결할 페이지. 아직 없으면 비워둔다 — 페이지가 생기면 여기만 채우면 링크가 된다. */
  href?: string;
  /** 현재 화면 강조(activeMode)와 매칭할 모드. */
  mode?: AppMode;
}

/**
 * 권한 → 탭 매칭 배열. 새 권한이 생기면 여기에 한 줄만 추가하면 스위처에 표시된다.
 * label/href 를 채우면 라벨·링크가 붙고, 여기에 없는 권한도 권한명 그대로 표시된다.
 * 배열 순서 = 탭이 보이는 좌→우 순서.
 */
const ROLE_TABS: RoleTab[] = [
  { role: "USER", label: "일반 유저", href: "/", mode: "user" },
  { role: "MANAGER", label: "박람회 관리자", href: "/manager/events", mode: "manager" },
  { role: "ADMIN", label: "전체 관리자", href: "/admin/events", mode: "admin" },
  { role: "DEVELOPER", label: "개발자", href: "/developer", mode: "developer" },
];

export function ModeSwitcher({
  activeMode,
  roles,
  compact = false,
}: ModeSwitcherProps) {
  // 등록된 권한은 정의 순서대로, 등록되지 않은 새 권한은 뒤에 권한명 그대로 붙인다.
  const known = ROLE_TABS.filter((tab) => roles.includes(tab.role));
  const knownRoles = new Set(known.map((tab) => tab.role));
  const extra: RoleTab[] = roles
    .filter((role) => !knownRoles.has(role))
    .map((role) => ({ role, label: role }));
  const tabs = [...known, ...extra];

  // 전환할 곳이 없으면(예: USER 권한만 보유) 스위처를 아예 숨긴다.
  if (tabs.length < 2) return null;

  return (
    <nav aria-label="화면 모드 전환" className={styles.switcher} data-compact={compact}>
      {tabs.map((tab) => {
        const current = tab.mode !== undefined && tab.mode === activeMode;
        return tab.href ? (
          <Link
            key={tab.role}
            href={tab.href}
            aria-current={current ? "page" : undefined}
            className={styles.link}
          >
            {tab.label}
          </Link>
        ) : (
          // 아직 링크가 없는 권한: 표시만 하고 이동하지 않는다.
          <span
            key={tab.role}
            aria-current={current ? "page" : undefined}
            className={styles.link}
          >
            {tab.label}
          </span>
        );
      })}
    </nav>
  );
}
