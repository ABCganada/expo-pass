export interface BannerAd {
  id: string;
  slotIds: string[];
  title: string;
  bannerImageUrl?: string;
  adImageUrl?: string;
  linkUrl: string;
  priority: number;
}
