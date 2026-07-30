import { getCsrfToken } from "@/features/shared/api/csrf";
import type { BannerPricingPolicy, BannerSlot, BannerSlotType, MarketerBannerAd } from "../types/marketerBanner";

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

  updateSlot: async (id: string, cmd: { name: string; maxCount: number; type: BannerSlotType }): Promise<BannerSlot> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/slots/${id}`, {
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify(cmd),
    }).then((res) => parseData<BannerSlot>(res));
  },

  deleteSlot: async (id: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/slots/${id}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      throw new Error(body?.message ?? "슬롯 삭제에 실패했습니다.");
    }
  },

  createSlot: async (cmd: { name: string; maxCount: number; type: BannerSlotType }): Promise<BannerSlot> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/slots`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify(cmd),
    }).then((res) => parseData<BannerSlot>(res));
  },

  getPoliciesBySlot: (slotId: string, signal?: AbortSignal): Promise<BannerPricingPolicy[]> =>
    fetch(`${BASE}/slots/${slotId}/policies`, { credentials: "include", signal })
      .then((res) => parseData<BannerPricingPolicy[]>(res)),

  createPolicy: async (slotId: string, cmd: { durationDays: number; price: number }): Promise<BannerPricingPolicy> => {
    const csrf = await getCsrfToken();
    return fetch(`${BASE}/slots/${slotId}/policies`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify(cmd),
    }).then((res) => parseData<BannerPricingPolicy>(res));
  },

  deletePolicy: async (slotId: string, policyId: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/slots/${slotId}/policies/${policyId}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      throw new Error(body?.message ?? "정책 삭제에 실패했습니다.");
    }
  },

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
