"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { authService } from "@/features/auth/services/authService";
import { MessengerWidget } from "@/features/chat/components/MessengerWidget/MessengerWidget";
import { MobileBottomNav } from "@/features/shell/components/MobileBottomNav";
import { AdminSidebar } from "@/features/admin/components/AdminSidebar";
import { AdminHeader } from "@/features/admin/components/AdminHeader";
import styles from "./layout.module.css";

export default function UserLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { status: authStatus, user, error: authError } = useAuth();
  const [isUserSidebarCollapsed, setIsUserSidebarCollapsed] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    if (authStatus === "unauthenticated") router.replace("/login");
  }, [authStatus, router]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const saved = localStorage.getItem("lastmission-sidebar-collapsed");
      if (saved !== null) setIsUserSidebarCollapsed(saved === "true");
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleToggleCollapse = () => {
    const collapsed = !isUserSidebarCollapsed;
    setIsUserSidebarCollapsed(collapsed);
    localStorage.setItem("lastmission-sidebar-collapsed", String(collapsed));
  };

  const handleLogout = () => {
    authService.redirectToLogout(`${window.location.origin}/login`);
  };

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
          <AdminSidebar
            mode="user"
            isOpen={sidebarOpen}
            onClose={() => setSidebarOpen(false)}
            isCollapsed={isUserSidebarCollapsed}
            onToggleCollapse={handleToggleCollapse}
            onLogout={handleLogout}
          />

          <div className={styles.main} data-collapsed={isUserSidebarCollapsed}>
            <AdminHeader
              activeMode="user"
              onMenuToggle={() => setSidebarOpen(!sidebarOpen)}
              user={user}
              onLogout={handleLogout}
            />

            <div className={styles.viewport}>
              <div className={styles.contentBox}>
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
