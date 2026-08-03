import type { AdminEventStatus } from "./adminEvent";

export type EventContentType = "DESCRIPTION" | "NOTICE" | "LOCATION_GUIDE";
export type EventImageType = "THUMBNAIL" | "GENERAL";

export interface AdminEventTicket {
  id: string;
  eventId: string;
  name: string;
  price: number;
  quantityTotal: number;
  quantityRemaining: number;
  maxPurchasePerUser: number;
  saleStartAt: string | null;
  saleEndAt: string | null;
  createdAt: string;
  deletedAt: string | null;
}

export interface AdminEventContentItem {
  id: string;
  contentType: EventContentType;
  content: string;
  updatedAt: string;
}

export interface AdminEventImageItem {
  id: string;
  eventId: string;
  imageUrl: string;
  imageType: EventImageType;
  displayOrder: number;
  createdAt: string;
}

export interface AdminEventDetail {
  id: string;
  title: string;
  categoryName: string;
  managerId: string;
  managerName: string | null;
  hostName: string | null;
  venueName: string | null;
  address: string | null;
  detailAddress: string | null;
  kakaoPlaceId: string | null;
  legalDongCode: string | null;
  latitude: number | null;
  longitude: number | null;
  startDate: string | null;
  endDate: string | null;
  status: AdminEventStatus;
  phase: string | null;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
  tickets: AdminEventTicket[];
  contents: AdminEventContentItem[];
  images: AdminEventImageItem[];
}

export interface UpdateEventPayload {
  title: string;
  categoryId: string;
  hostName: string | null;
  venueName: string | null;
  address: string | null;
  detailAddress: string | null;
  kakaoPlaceId: string | null;
  legalDongCode: string | null;
  latitude: number | null;
  longitude: number | null;
  startDate: string | null;
  endDate: string | null;
}

export interface CreateTicketPayload {
  name: string;
  price: number;
  quantityTotal: number;
  maxPurchasePerUser: number;
  saleStartAt: string | null;
  saleEndAt: string | null;
}

export interface UpdateTicketPayload {
  name: string;
  price: number;
  quantityTotal: number;
  maxPurchasePerUser: number;
  saleStartAt: string | null;
  saleEndAt: string | null;
}