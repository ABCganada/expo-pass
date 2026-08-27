import { getCsrfToken } from "@/features/shared/api/csrf";
import type {
  EventManagementDetail,
  EventImageItem,
  EventContentItem,
  EventTicket,
  CreateTicketPayload,
  EventContentType,
  EventImageType,
  UpdateEventPayload,
  UpdateTicketPayload,
} from "../types/eventManagementDetail";
import type { EventSummary } from "../types/eventCreate";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/manager/events`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

async function sendJson<T>(url: string, method: string, payload: unknown): Promise<T> {
  const csrf = await getCsrfToken();
  const res = await fetch(url, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  return parseData<T>(res);
}

async function patchAction<T>(url: string): Promise<T> {
  const csrf = await getCsrfToken();
  const res = await fetch(url, {
    method: "PATCH",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
  });
  return parseData<T>(res);
}

export const managerEventDetailService = {
  getManagerEventDetail: (eventId: string, signal?: AbortSignal): Promise<EventManagementDetail> =>
    fetch(`${BASE}/${eventId}`, { credentials: "include", signal }).then((res) =>
      parseData<EventManagementDetail>(res),
    ),

  updateEvent: (eventId: string, payload: UpdateEventPayload): Promise<EventSummary> =>
    sendJson(`${BASE}/${eventId}`, "PATCH", payload),

  cancelManagerEvent: (eventId: string): Promise<EventSummary> => patchAction(`${BASE}/${eventId}/cancel`),

  upsertContents: (
    eventId: string,
    contents: Partial<Record<EventContentType, string>>,
  ): Promise<EventContentItem[]> => sendJson(`${BASE}/${eventId}/contents`, "PUT", { contents }),

  uploadImage: async (eventId: string, file: File, imageType: EventImageType): Promise<EventImageItem> => {
    const csrf = await getCsrfToken();
    const formData = new FormData();
    formData.append("file", file);
    formData.append("imageType", imageType);
    const res = await fetch(`${BASE}/${eventId}/images`, {
      method: "POST",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
      body: formData,
    });
    return parseData<EventImageItem>(res);
  },

  deleteImage: async (eventId: string, imageId: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${eventId}/images/${imageId}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    await parseData<void>(res);
  },

  createTicket: (eventId: string, payload: CreateTicketPayload): Promise<EventTicket> =>
    sendJson(`${BASE}/${eventId}/tickets`, "POST", payload),

  updateTicket: (eventId: string, ticketId: string, payload: UpdateTicketPayload): Promise<EventTicket> =>
    sendJson(`${BASE}/${eventId}/tickets/${ticketId}`, "PUT", payload),

  deleteTicket: async (eventId: string, ticketId: string): Promise<void> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${eventId}/tickets/${ticketId}`, {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    await parseData<void>(res);
  },
};