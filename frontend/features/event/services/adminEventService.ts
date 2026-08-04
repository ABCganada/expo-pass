import { getCsrfToken } from "@/features/shared/api/csrf";
import type { EventListItem, AdminEventStatus } from "../types/eventList";
import type { EventSummary } from "../types/eventCreate";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/admin/events`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const adminEventService = {
  getAdminEvents: (status: AdminEventStatus | undefined, signal?: AbortSignal): Promise<EventListItem[]> => {
    const url = status ? `${BASE}?status=${status}` : BASE;
    return fetch(url, { credentials: "include", signal }).then((res) => parseData<EventListItem[]>(res));
  },

  deleteAdminEvent: async (eventId: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${eventId}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    await parseData<void>(res);
  },

  changeManager: async (eventId: string, managerId: string): Promise<EventSummary> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${eventId}/manager`, {
      method: "PATCH",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({ managerId }),
    });
    return parseData<EventSummary>(res);
  },
};