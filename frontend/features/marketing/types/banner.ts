export interface BannerAd {
  id: string;
  slotIds: string[];
  slotTypes: string[];
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
}
