import { getCsrfToken } from "@/features/shared/api/csrf";
import type {
  BannerAdStats,
  BannerSlot,
  MarketerBannerAd,
  RegisterAdCommand,
  UpdateAdCommand,
} from "../types/marketerBanner";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const marketerBannerService = {
  getMyAds: (signal?: AbortSignal): Promise<MarketerBannerAd[]> =>
    fetch(`${BASE}/manager/banner-ads`, { credentials: "include", signal })
      .then((res) => parseData<MarketerBannerAd[]>(res)),

  getSlots: (signal?: AbortSignal): Promise<BannerSlot[]> =>
    fetch(`${BASE}/manager/banner-ads/slots`, { credentials: "include", signal })
      .then((res) => parseData<BannerSlot[]>(res)),

  uploadImage: async (file: File): Promise<string> => {
    const csrf = await getCsrfToken();
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${BASE}/manager/banner-ads/images`, {
      method: "POST",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
      body: form,
    });
    const body = await res.json();
    if (!res.ok || !body.success) throw new Error(body.message ?? "이미지 업로드에 실패했습니다.");
    return body.data.imageUrl as string;
  },

  registerAd: async (cmd: RegisterAdCommand): Promise<MarketerBannerAd> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/manager/banner-ads`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify(cmd),
    }).then((res) => parseData<MarketerBannerAd>(res));
  },

  updateAd: async (id: string, cmd: UpdateAdCommand): Promise<MarketerBannerAd> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/manager/banner-ads/${id}`, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify(cmd),
    }).then((res) => parseData<MarketerBannerAd>(res));
  },

  getStatsByDateRange: (
    id: string,
    from: string,
    to: string,
    signal?: AbortSignal,
  ): Promise<BannerAdStats> =>
    fetch(`${BASE}/manager/banner-ads/${id}/stats?from=${from}&to=${to}`, {
      credentials: "include",
      signal,
    }).then((res) => parseData<BannerAdStats>(res)),

  downloadStatsXlsx: async (id: string, from: string, to: string): Promise<void> => {
    const res = await fetch(
      `${BASE}/manager/banner-ads/${id}/stats/export?from=${from}&to=${to}`,
      { credentials: "include" },
    );
    if (!res.ok) throw new Error("다운로드에 실패했습니다.");
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `banner-stats-${from}-${to}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  },
};
