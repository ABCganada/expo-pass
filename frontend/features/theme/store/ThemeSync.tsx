"use client";

import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/features/store/hooks";
import {
  DEFAULT_THEME,
  isThemeName,
  themeRegistry,
} from "../themeRegistry";
import { initializeTheme } from "./themeSlice";

const STORAGE_KEY = "lastmission-theme";

/**
 * Redux의 테마 상태를 브라우저 DOM과 저장소에 동기화한다.
 *
 * reducer에서는 window/localStorage를 만지지 않는다. 서버 렌더링이 끝난 뒤 이
 * 클라이언트 경계에서만 사용자 선호도를 읽고 <html>에 적용한다.
 */
export function ThemeSync() {
  const dispatch = useAppDispatch();
  const { name, hydrated } = useAppSelector((state) => state.theme);

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    const initial = isThemeName(stored)
      ? stored
      : window.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : DEFAULT_THEME;
    dispatch(initializeTheme(initial));
  }, [dispatch]);

  useEffect(() => {
    if (!hydrated) return;
    document.documentElement.dataset.theme = name;
    document.documentElement.style.colorScheme =
      themeRegistry[name].colorScheme;
    window.localStorage.setItem(STORAGE_KEY, name);
  }, [hydrated, name]);

  return null;
}
