"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { AdminHeader } from "@/features/admin/components/AdminHeader";
import { AdminSidebar } from "@/features/admin/components/AdminSidebar";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { authService } from "@/features/auth/services/authService";
import styles from "../admin/layout.module.css";

export default function DeveloperLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { status: authStatus, user, error: authError } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const hasDeveloperRole = user?.roles.includes("DEVELOPER") ?? false;

  useEffect(() => {
    if (authStatus === "unauthenticated") router.replace("/login");
    else if (authStatus === "authenticated" && !hasDeveloperRole) router.replace("/");
  }, [authStatus, hasDeveloperRole, router]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (window.innerWidth >= 1024) setSidebarOpen(true);
      const saved = localStorage.getItem("lastmission-sidebar-collapsed");
      if (saved !== null) setIsSidebarCollapsed(saved === "true");
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const handleToggleCollapse = () => {
    const collapsed = !isSidebarCollapsed;
    setIsSidebarCollapsed(collapsed);
    localStorage.setItem("lastmission-sidebar-collapsed", String(collapsed));
  };

  const handleLogout = () => authService.redirectToLogout(`${window.location.origin}/login`);

  return (
    <div className={styles.shell}>
      {authStatus === "loading" || authStatus === "unauthenticated" ? (
        <div className={styles.state}>로그인 확인 중...</div>
      ) : authStatus === "error" ? (
        <div className={styles.state}>
          <p className={styles.stateTitle}>로그인 상태를 확인할 수 없습니다.</p>
          <p className={styles.stateDetail}>{authError}</p>
        </div>
      ) : !hasDeveloperRole ? (
        <div className={styles.state}>일반 사용자 화면으로 이동 중...</div>
      ) : (
        <>
          <AdminSidebar
            mode="developer"
            isOpen={sidebarOpen}
            onClose={() => setSidebarOpen(false)}
            isCollapsed={isSidebarCollapsed}
            onToggleCollapse={handleToggleCollapse}
            onLogout={handleLogout}
          />
          <div className={styles.main} data-collapsed={isSidebarCollapsed}>
            <AdminHeader
              activeMode="developer"
              onMenuToggle={() => setSidebarOpen(!sidebarOpen)}
              user={user}
              onLogout={handleLogout}
            />
            <main className={styles.content}>{children}</main>
          </div>
        </>
      )}
    </div>
  );
}
