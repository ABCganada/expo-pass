"use client";

import { LoginButton } from "@/features/coderhan/components/LoginButton";
import styles from "./LoginForm.module.css";

export function LoginForm() {
  return (
    <div className={styles.card}>
      {/* 로고 영역 */}
      <div className={styles.logoArea}>
        <div className={styles.logoBadge}>L</div>
        <h1 className={styles.brand}>Last Mission</h1>
      </div>

      <LoginButton className={styles.loginButton} label="로그인" />
    </div>
  );
}
