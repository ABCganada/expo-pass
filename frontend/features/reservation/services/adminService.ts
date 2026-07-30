import { getCsrfToken } from "@/features/shared/api/csrf";
import type { Attendee, CheckinResponse, EventReservationSummary } from "../types/admin";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/admin`;

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

  getEventSummary: (eventId: string, signal?: AbortSignal): Promise<EventReservationSummary> =>
    fetch(`${BASE}/reservations/events/${eventId}/summary`, { credentials: "include", signal }).then((res) =>
      parseData<EventReservationSummary>(res),
    ),

  checkin: async (qrCodeHash: string, signal?: AbortSignal): Promise<CheckinResponse> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/reservations/checkin`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify({ qrCodeHash }),
      signal,
    });
    return parseData<CheckinResponse>(res);
  },
};
