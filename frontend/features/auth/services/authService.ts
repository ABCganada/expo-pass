import { CurrentUser, MeResponse } from "../types/auth";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

function buildApiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

function isMeResponse(value: unknown): value is MeResponse {
  if (typeof value !== "object" || value === null) return false;

  const response = value as Record<string, unknown>;
  return (
    typeof response.id === "string" &&
    typeof response.authUuid === "string" &&
    typeof response.name === "string" &&
    typeof response.email === "string" &&
    Array.isArray(response.roles) &&
    response.roles.every((role) => typeof role === "string")
  );
}

export const authService = {
  async getCurrentUser(signal?: AbortSignal): Promise<CurrentUser | null> {
    const response = await fetch(buildApiUrl("/api/me"), {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      signal,
    });

    if (response.status === 401) return null;

    if (!response.ok) {
      throw new Error(`인증 확인에 실패했습니다. (${response.status})`);
    }

    const body: unknown = await response.json();
    if (!isMeResponse(body)) {
      throw new Error("인증 서버 응답 형식이 올바르지 않습니다.");
    }

    return body;
  },

  redirectToLogin(returnTo: string): void {
    const query = new URLSearchParams({ returnTo });
    window.location.assign(buildApiUrl(`/auth/login?${query.toString()}`));
  },

  redirectToLogout(returnTo: string): void {
    const query = new URLSearchParams({ returnTo });
    window.location.assign(buildApiUrl(`/auth/logout?${query.toString()}`));
  },
};
