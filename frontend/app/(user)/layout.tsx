"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { ModeSwitcher } from "@/features/auth/components/ModeSwitcher";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { authService } from "@/features/auth/services/authService";
import { MessengerWidget } from "@/features/chat/components/MessengerWidget/MessengerWidget";
import { MobileBottomNav } from "@/features/shell/components/MobileBottomNav";
import { UserSidebar } from "@/features/shell/components/UserSidebar";
import { ThemeToggle } from "@/features/theme/components/ThemeToggle/ThemeToggle";
import styles from "./layout.module.css";

export default function UserLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { status: authStatus, user, error: authError } = useAuth();
  const [isUserSidebarCollapsed, setIsUserSidebarCollapsed] = useState(false);

  useEffect(() => {
    if (authStatus === "unauthenticated") router.replace("/login");
  }, [authStatus, router]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const saved = localStorage.getItem("lastmission-user-sidebar-collapsed");
      if (saved !== null) setIsUserSidebarCollapsed(saved === "true");
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleToggleCollapse = () => {
    const collapsed = !isUserSidebarCollapsed;
    setIsUserSidebarCollapsed(collapsed);
    localStorage.setItem("lastmission-user-sidebar-collapsed", String(collapsed));
  };

  const handleLogout = () => {
    authService.redirectToLogout(`${window.location.origin}/login`);
  };

  const displayName = user?.name?.trim() || user?.email?.trim() || null;

  return (
    <div className={styles.shell}>
      {authStatus === "loading" || authStatus === "unauthenticated" ? (
        <div className={styles.state}>로그인 확인 중...</div>
      ) : authStatus === "error" ? (
        <div className={styles.state}>
          <p className={styles.stateTitle}>로그인 상태를 확인할 수 없습니다.</p>
          <p className={styles.stateDetail}>{authError}</p>
        </div>
      ) : (
        <>
          <UserSidebar
            isCollapsed={isUserSidebarCollapsed}
            onToggleCollapse={handleToggleCollapse}
            onLogout={handleLogout}
          />

          <div className={styles.main}>
            <header className={styles.desktopHeader}>
              <div className={styles.titleGroup}>
                <h2 className={styles.title}>홈</h2>
                <span className={styles.subtitle}>서비스 홈</span>
              </div>
              <div className={styles.actions}>
                <ModeSwitcher activeMode="user" roles={user?.roles ?? []} />
                <div className={styles.profileGroup}>
                  {displayName ? (
                    <div className={styles.profile}>
                      <div className={styles.avatar}>{displayName[0]}</div>
                      <div className={styles.profileText}>
                        <span className={styles.profileName}>{displayName}</span>
                        <span className={styles.profileGoal}>{user?.email}</span>
                      </div>
                    </div>
                  ) : null}
                  <button onClick={handleLogout} className={styles.logoutButton}>로그아웃</button>
                  <ThemeToggle />
                </div>
              </div>
            </header>

            <div className={styles.viewport}>
              <div className={styles.contentBox}>
                <div className={styles.mobileHeader}>
                  <h1 className={styles.mobileTitle}>홈</h1>
                  <div className={styles.mobileActions}>
                    <ModeSwitcher activeMode="user" roles={user?.roles ?? []} compact />
                    <button onClick={handleLogout} className={styles.mobileLogout}>로그아웃</button>
                    <ThemeToggle />
                  </div>
                </div>
                {children}
                <MobileBottomNav />
              </div>
            </div>
          </div>

          <MessengerWidget
            currentUserId={user?.id ?? ""}
            roles={user?.roles ?? []}
            mode="user"
          />
        </>
      )}
    </div>
  );
}
