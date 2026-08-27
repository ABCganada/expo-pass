import { getCsrfToken } from "@/features/shared/api/csrf";
import type { BannerSlot, MarketerBannerAd } from "../types/marketerBanner";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/admin/banner`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const adminBannerService = {
  getAllAds: (signal?: AbortSignal): Promise<MarketerBannerAd[]> =>
    fetch(`${BASE}/ads`, { credentials: "include", signal })
      .then((res) => parseData<MarketerBannerAd[]>(res)),

  getSlots: (signal?: AbortSignal): Promise<BannerSlot[]> =>
    fetch(`${BASE}/slots`, { credentials: "include", signal })
      .then((res) => parseData<BannerSlot[]>(res)),

  approveAd: async (id: string): Promise<MarketerBannerAd> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/ads/${id}/approve`, {
      method: "POST",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    }).then((res) => parseData<MarketerBannerAd>(res));
  },

  rejectAd: async (id: string): Promise<MarketerBannerAd> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/ads/${id}/reject`, {
      method: "POST",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    }).then((res) => parseData<MarketerBannerAd>(res));
  },

  deleteAd: async (id: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/ads/${id}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      throw new Error(body?.message ?? "삭제에 실패했습니다.");
    }
  },
};
