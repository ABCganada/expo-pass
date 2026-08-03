import { getCsrfToken } from "@/features/shared/api/csrf";
import type { EventListItem, AdminEventStatus } from "../types/eventList";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/manager/events`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const managerEventService = {
  getManagerEvents: (status: AdminEventStatus | undefined, signal?: AbortSignal): Promise<EventListItem[]> => {
    const url = status ? `${BASE}?status=${status}` : BASE;
    return fetch(url, { credentials: "include", signal }).then((res) => parseData<EventListItem[]>(res));
  },

  deleteManagerEvent: async (eventId: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${eventId}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    await parseData<void>(res);
  },
};