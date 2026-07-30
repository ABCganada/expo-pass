import type { EventCategory, EventListItem } from "../types/event";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const eventService = {
  getCategories: (signal?: AbortSignal): Promise<EventCategory[]> =>
    fetch(`${BASE}/events/categories`, { credentials: "include", signal }).then((res) =>
      parseData<EventCategory[]>(res),
    ),

  getEvents: (categoryId: string | undefined, signal?: AbortSignal): Promise<EventListItem[]> => {
    const url = categoryId ? `${BASE}/events?categoryId=${encodeURIComponent(categoryId)}` : `${BASE}/events`;
    return fetch(url, { credentials: "include", signal }).then((res) => parseData<EventListItem[]>(res));
  },
};