export type BannerAdStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED" | "CANCELLED" | "REFUNDED";
export type BannerSlotType = "BANNER" | "TAB";

export const BANNER_AD_STATUS_LABEL: Record<BannerAdStatus, string> = {
  PENDING: "결제 대기",
  APPROVED: "진행 중",
  REJECTED: "거절됨",
  EXPIRED: "종료됨",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

export const BANNER_SLOT_TYPE_LABEL: Record<BannerSlotType, string> = {
  BANNER: "배너형",
  TAB: "광고탭형",
};

export interface MarketerBannerAd {
  id: string;
  orderId: string;
  slotIds: string[];
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  status: BannerAdStatus;
  startsAt: string;
  endsAt: string;
  createdBy: string;
  createdAt: string;
  totalAmount: number;
}

export interface BannerSlot {
  id: string;
  name: string;
  maxCount: number;
  type: BannerSlotType;
  pricePerDay: number;
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
  startsAt: string;
  endsAt: string;
}

export interface UpdateAdCommand {
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  startsAt: string;
  endsAt: string;
}
