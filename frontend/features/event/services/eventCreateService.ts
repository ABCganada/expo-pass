import { getCsrfToken } from "@/features/shared/api/csrf";
import type { CreateEventPayload, CreatedEvent } from "../types/eventCreate";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/admin/events`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const eventCreateService = {
  createDraftEvent: async (payload: CreateEventPayload): Promise<CreatedEvent> => {
    const csrf = await getCsrfToken();
    const res = await fetch(BASE, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify(payload),
    });
    return parseData<CreatedEvent>(res);
  },

  changeManager: async (eventId: string, managerId: string): Promise<CreatedEvent> => {
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
    return parseData<CreatedEvent>(res);
  },
};