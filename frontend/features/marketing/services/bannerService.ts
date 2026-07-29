import type { BannerAd } from "../types/banner";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const buildApiUrl = (path: string) => `${API_BASE_URL}${path}`;

export const bannerService = {
  async getActiveBanners(signal?: AbortSignal): Promise<BannerAd[]> {
    const res = await fetch(buildApiUrl("/api/v1/banners"), { credentials: "include", signal });
    if (!res.ok) throw new Error("배너 목록을 불러오지 못했습니다.");
    const body: { data: BannerAd[] } = await res.json();
    return body.data;
  },

  async recordImpression(adId: string): Promise<void> {
    await fetch(buildApiUrl(`/api/v1/banners/${adId}/impressions`), {
      method: "POST",
      credentials: "include",
    });
  },

  async recordClick(adId: string): Promise<void> {
    await fetch(buildApiUrl(`/api/v1/banners/${adId}/clicks`), {
      method: "POST",
      credentials: "include",
    });
  },
};
