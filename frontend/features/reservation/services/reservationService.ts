import { getCsrfToken } from "@/features/shared/api/csrf";
import type {
  CreateOrderItemInput,
  OrderDetail,
  OrderSummary,
  QrTicket,
} from "../types/reservation";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const reservationService = {
  getMyOrders: (signal?: AbortSignal): Promise<OrderSummary[]> =>
    fetch(`${BASE}/reservations/me`, { credentials: "include", signal }).then((res) =>
      parseData<OrderSummary[]>(res),
    ),

  getMyQrTickets: (signal?: AbortSignal): Promise<QrTicket[]> =>
    fetch(`${BASE}/reservations/me/qr-tickets`, { credentials: "include", signal }).then((res) =>
      parseData<QrTicket[]>(res),
    ),

  createOrder: async (
    eventId: string,
    items: CreateOrderItemInput[],
    signal?: AbortSignal,
  ): Promise<OrderDetail> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/reservations`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify({
        eventId: Number(eventId),
        items: items.map((item) => ({
          ticketId: Number(item.ticketId),
          unitPrice: item.unitPrice,
          quantity: item.quantity,
        })),
      }),
      signal,
    });
    return parseData<OrderDetail>(res);
  },
};
