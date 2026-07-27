"use client";

import { Moon, Sun } from "lucide-react";
import { useAppDispatch, useAppSelector } from "@/features/store/hooks";
import { toggleTheme } from "../../store/themeSlice";
import styles from "./ThemeToggle.module.css";

export function ThemeToggle() {
  const dispatch = useAppDispatch();
  const theme = useAppSelector((state) => state.theme.name);
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      className={styles.button}
      onClick={() => dispatch(toggleTheme())}
      aria-label={isDark ? "라이트 모드로 변경" : "다크 모드로 변경"}
      title={isDark ? "라이트 모드" : "다크 모드"}
    >
      {isDark ? <Sun aria-hidden="true" /> : <Moon aria-hidden="true" />}
    </button>
  );
}
