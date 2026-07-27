"use client";

import { queryErrorMessage } from "@/features/store/api/queryError";
import { useGetCurrentUserQuery } from "../api/authApi";

export type AuthStatus =
  | "loading"
  | "authenticated"
  | "unauthenticated"
  | "error";

export function useAuth() {
  const { data, isLoading, isError, error } = useGetCurrentUserQuery(undefined, {
    refetchOnFocus: true,
    refetchOnReconnect: true,
  });

  const status: AuthStatus = isLoading
    ? "loading"
    : isError
      ? "error"
      : data
        ? "authenticated"
        : "unauthenticated";

  return {
    status,
    user: data ?? null,
    error: isError
      ? queryErrorMessage(error, "인증 상태를 확인할 수 없습니다.")
      : null,
  };
}
