export interface EventCategory {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export interface EventListItem {
  id: string;
  title: string;
  categoryName: string;
  venueName: string;
  startDate: string;
  endDate: string;
  phase: string;
  viewCount: number;
  thumbnailUrl: string | null;
}

export interface EventTicket {
  id: string;
  name: string;
  price: number;
  quantityRemaining: number;
  maxPurchasePerUser: number;
  saleStartAt: string;
  saleEndAt: string;
}

export type EventContentType = "DESCRIPTION" | "NOTICE" | "LOCATION_GUIDE";

export interface EventContentItem {
  id: string;
  contentType: EventContentType;
  content: string;
  updatedAt: string;
}

export type EventImageType = "THUMBNAIL" | "GENERAL";

export interface EventImageItem {
  id: string;
  imageUrl: string;
  imageType: EventImageType;
  displayOrder: number;
}

// legalDongCode는 화면에 노출하지 않는 내부 데이터
export interface EventDetail {
  id: string;
  title: string;
  categoryName: string;
  hostName: string | null;
  venueName: string | null;
  address: string | null;
  detailAddress: string | null;
  kakaoPlaceId: string | null;
  latitude: number | null;
  longitude: number | null;
  startDate: string | null;
  endDate: string | null;
  status: "DRAFT" | "PUBLISHED" | "CANCELLED";
  phase: string;
  viewCount: number;
  tickets: EventTicket[];
  contents: EventContentItem[];
  images: EventImageItem[];
}

export interface BookmarkedEvent {
  id: string;
  title: string;
  categoryName: string;
  startDate: string;
  endDate: string;
  phase: string;
}