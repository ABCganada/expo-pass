import type { AdminEventListItem, AdminEventStatus } from "../types/adminEvent";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/manager`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const adminEventService = {
  getAdminEvents: (status: AdminEventStatus | undefined, signal?: AbortSignal): Promise<AdminEventListItem[]> => {
    const url = status ? `${BASE}/events?status=${status}` : `${BASE}/events`;
    return fetch(url, { credentials: "include", signal }).then((res) => parseData<AdminEventListItem[]>(res));
  },
};