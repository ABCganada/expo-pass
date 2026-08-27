"use client";

import React from "react";
import { LoginForm } from "@/features/auth/components/LoginForm";
import styles from "./page.module.css";

export default function LoginPage() {
  return (
    <div className={styles.page}>
      {/* 장식용 은은한 그라데이션 블러 서클 (미적인 요소 추가) */}
      <div className={`${styles.glow} ${styles.glowTop}`} />
      <div className={`${styles.glow} ${styles.glowBottom}`} />

      {/* 로그인 폼 */}
      <LoginForm />
    </div>
  );
}
