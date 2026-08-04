import type { AdminEventListItem, EventSummary, EventTicketOption } from "../types/eventLookup";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

// Event 도메인의 공개 API(누구나 호출 가능)를 그대로 불러온다.
// Event의 프론트 기능을 대신 만드는 게 아니라, Reservation 화면 표시용으로만 쓴다.
export const eventLookupService = {
  getEvent: (eventId: string, signal?: AbortSignal): Promise<EventSummary> =>
    fetch(`${BASE}/events/${eventId}`, { credentials: "include", signal }).then((res) =>
      parseData<EventSummary>(res),
    ),

  getEventTickets: (eventId: string, signal?: AbortSignal): Promise<EventTicketOption[]> =>
    fetch(`${BASE}/events/${eventId}/tickets`, { credentials: "include", signal }).then((res) =>
      parseData<EventTicketOption[]>(res),
    ),

  // MANAGER 컨텍스트용 — 호출자 본인이 담당하는 행사만 반환한다.
  getAdminEvents: (signal?: AbortSignal): Promise<AdminEventListItem[]> =>
    fetch(`${BASE}/manager/events`, { credentials: "include", signal }).then((res) =>
      parseData<AdminEventListItem[]>(res),
    ),

  // ADMIN 전용 — 전체 행사를 반환한다.
  getAllEvents: (signal?: AbortSignal): Promise<AdminEventListItem[]> =>
    fetch(`${BASE}/admin/events`, { credentials: "include", signal }).then((res) =>
      parseData<AdminEventListItem[]>(res),
    ),
};
