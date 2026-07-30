// Event 도메인의 공개 API 응답을 그대로 미러링한 타입입니다.
// Event 도메인 소유가 아니라, Reservation이 자기 화면(예약 카드 등)에 행사/티켓 이름을
// 표시하기 위해 Event의 공개 API를 호출할 때 쓰는 조회 전용 타입입니다.
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
