export type BannerAdStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED";

export const BANNER_AD_STATUS_LABEL: Record<BannerAdStatus, string> = {
  PENDING: "검토 중",
  APPROVED: "진행 중",
  REJECTED: "거절됨",
  EXPIRED: "종료됨",
};

export interface MarketerBannerAd {
  id: string;
  slotId: string;
  title: string;
  imageUrl: string;
  linkUrl: string;
  priority: number;
  status: BannerAdStatus;
  startsAt: string;
  endsAt: string;
  createdBy: string;
  createdAt: string;
}

export interface BannerSlot {
  id: string;
  name: string;
  maxCount: number;
}

export interface BannerAdStats {
  adId: string;
  impressions: number;
  clicks: number;
  ctr: number;
}

export interface RegisterAdCommand {
  slotId: string;
  title: string;
  imageUrl: string;
  linkUrl: string;
  priority: number;
  startsAt: string;
  endsAt: string;
}

export interface UpdateAdCommand {
  title: string;
  imageUrl: string;
  linkUrl: string;
  priority: number;
  startsAt: string;
  endsAt: string;
}
