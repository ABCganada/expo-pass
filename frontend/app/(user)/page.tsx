"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import styles from "./page.module.css";

export default function Home() {
  const { user } = useAuth();
  const displayName = user?.name?.trim() || user?.email?.trim() || "사용자";

  return (
    <main className={styles.page}>
      <section className={styles.welcomeCard}>
        <p className={styles.eyebrow}>WELCOME</p>
        <h1 className={styles.title}>{displayName}님, 반갑습니다.</h1>
        <p className={styles.description}>
          새로운 기능이 준비되는 동안 메신저를 사용할 수 있습니다.
        </p>
      </section>

      <section className={styles.accountCard}>
        <h2 className={styles.sectionTitle}>내 정보</h2>
        <dl className={styles.accountList}>
          <div><dt>이름</dt><dd>{user?.name || "-"}</dd></div>
          <div><dt>이메일</dt><dd>{user?.email || "-"}</dd></div>
          <div><dt>역할</dt><dd>{user?.roles.join(", ") || "-"}</dd></div>
        </dl>
      </section>
    </main>
  );
}
