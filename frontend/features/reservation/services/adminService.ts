import { getCsrfToken } from "@/features/shared/api/csrf";
import type { Attendee, CheckinProgress, CheckinResponse, DailyReservationCount, EventReservationSummary } from "../types/admin";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/manager`;
const ADMIN_BASE = `${API_BASE_URL}/api/v1/admin`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const adminService = {
  getEventAttendees: (eventId: string, signal?: AbortSignal): Promise<Attendee[]> =>
    fetch(`${BASE}/reservations/events/${eventId}/attendees`, { credentials: "include", signal }).then((res) =>
      parseData<Attendee[]>(res),
    ),

  // 매니저 컨텍스트용 — 본인이 담당하는 행사가 아니면 거부된다(ADMIN도 예외 없음).
  getEventSummary: (eventId: string, signal?: AbortSignal): Promise<EventReservationSummary> =>
    fetch(`${BASE}/reservations/events/${eventId}/summary`, { credentials: "include", signal }).then((res) =>
      parseData<EventReservationSummary>(res),
    ),

  // ADMIN 전용 — 소유권 무관하게 전체 행사 대상으로 조회한다.
  getEventSummaryForAdmin: (eventId: string, signal?: AbortSignal): Promise<EventReservationSummary> =>
    fetch(`${ADMIN_BASE}/reservations/events/${eventId}/summary`, { credentials: "include", signal }).then((res) =>
      parseData<EventReservationSummary>(res),
    ),

  getCheckinProgress: (eventId: string, signal?: AbortSignal): Promise<CheckinProgress> =>
    fetch(`${BASE}/reservations/events/${eventId}/checkin-status`, { credentials: "include", signal }).then((res) =>
      parseData<CheckinProgress>(res),
    ),

  getDailyReservationCounts: (eventId: string, signal?: AbortSignal): Promise<DailyReservationCount[]> =>
    fetch(`${BASE}/reservations/events/${eventId}/daily-counts`, { credentials: "include", signal }).then((res) =>
      parseData<DailyReservationCount[]>(res),
    ),

  getDailyReservationCountsForAdmin: (eventId: string, signal?: AbortSignal): Promise<DailyReservationCount[]> =>
    fetch(`${ADMIN_BASE}/reservations/events/${eventId}/daily-counts`, { credentials: "include", signal }).then((res) =>
      parseData<DailyReservationCount[]>(res),
    ),

  checkin: async (qrCodeHash: string, eventId: string, signal?: AbortSignal): Promise<CheckinResponse> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/reservations/checkin`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify({ qrCodeHash, eventId }),
      signal,
    });
    return parseData<CheckinResponse>(res);
  },
};
