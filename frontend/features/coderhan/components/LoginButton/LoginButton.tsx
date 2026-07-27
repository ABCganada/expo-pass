"use client";

/**
 * coder-han 중앙 인증(Keycloak) 로그인 버튼.
 *
 * 백엔드의 /auth/login 으로 브라우저를 리다이렉트한다.
 * (아이디/비번 폼이 아니라 리다이렉트 방식 — 로그인 완료 후 returnTo 로 돌아온다.)
 *
 */

import { authService } from "@/features/auth/services/authService";
import { LogIn } from "lucide-react";
import styles from "./LoginButton.module.css";

interface LoginButtonProps {
  /** 로그인 완료 후 돌아올 주소. 기본값은 현재 사이트의 홈. */
  returnTo?: string;
  /** 버튼 라벨. 기본값 "로그인". */
  label?: string;
  className?: string;
}

export function LoginButton({
  returnTo,
  label = "로그인",
  className,
}: LoginButtonProps) {
  const handleLogin = () => {
    const target = returnTo ?? `${window.location.origin}/`;
    authService.redirectToLogin(target);
  };

  return (
    <button
      type="button"
      onClick={handleLogin}
      className={[styles.button, className].filter(Boolean).join(" ")}
    >
      <LogIn aria-hidden="true" className={styles.icon} />
      {label}
    </button>
  );
}
