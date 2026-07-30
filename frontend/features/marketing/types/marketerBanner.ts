export type BannerAdStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED";
export type BannerSlotType = "BANNER" | "TAB";

export const BANNER_AD_STATUS_LABEL: Record<BannerAdStatus, string> = {
  PENDING: "검토 중",
  APPROVED: "진행 중",
  REJECTED: "거절됨",
  EXPIRED: "종료됨",
};

export const BANNER_SLOT_TYPE_LABEL: Record<BannerSlotType, string> = {
  BANNER: "배너형",
  TAB: "광고탭형",
};

export interface MarketerBannerAd {
  id: string;
  slotIds: string[];
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  priority: number;
  status: BannerAdStatus;
  startsAt: string;
  endsAt: string;
  createdBy: string;
  createdAt: string;
  totalAmount?: number;
}

export interface BannerSlot {
  id: string;
  name: string;
  maxCount: number;
  type: BannerSlotType;
}

export interface SlotPolicy {
  id: string;
  durationDays: number;
  price: number;
}

export interface BannerSlotWithPolicies extends BannerSlot {
  policies: SlotPolicy[];
}

export interface BannerPricingPolicy {
  id: string;
  slotId: string;
  durationDays: number;
  price: number;
  createdAt: string;
}

export interface BannerAdStats {
  adId: string;
  impressions: number;
  clicks: number;
  ctr: number;
}

export interface RegisterAdCommand {
  slotIds: string[];
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  priority: number;
  startsAt: string;
  endsAt: string;
}

export interface UpdateAdCommand {
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  priority: number;
  startsAt: string;
  endsAt: string;
}
