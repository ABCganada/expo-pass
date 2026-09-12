// Event 도메인의 공개 API 응답을 그대로 미러링한 타입입니다.
// Event 도메인 소유가 아니라, Reservation이 자기 화면(예약 카드 등)에 행사/티켓 이름을
// 표시하기 위해 Event의 공개 API를 호출할 때 쓰는 조회 전용 타입입니다.
export type EventImageType = "THUMBNAIL" | "GENERAL";

export interface EventImageItem {
  id: string;
  imageUrl: string;
  imageType: EventImageType;
  displayOrder: number;
}

export interface EventSummary {
  id: string;
  title: string;
  categoryName: string;
  hostName: string;
  venueName: string;
  address: string;
  detailAddress: string;
  startDate: string;
  endDate: string;
  status: string;
  phase: string;
  viewCount: number;
  images: EventImageItem[];
}

export interface EventTicketOption {
  id: string;
  name: string;
  price: number;
  quantityRemaining: number;
  maxPurchasePerUser: number;
  saleStartAt: string;
  saleEndAt: string;
}

export interface AdminEventListItem {
  id: string;
  title: string;
  categoryName: string;
  managerId: string;
  status: string;
  startDate: string;
  endDate: string;
  phase: string;
  viewCount: number;
  thumbnailUrl: string | null;
}

/** GET /api/v1/events (공개, 게시된 행사만) 목록 응답 — 예약은 게시된 행사에서만 발생하므로
 *  DRAFT까지 섞여 나오는 관리자용 목록 대신 이 유저용 목록을 쓰는 화면(행사별 예약 현황 등)이 있다. */
export interface PublishedEventListItem {
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
