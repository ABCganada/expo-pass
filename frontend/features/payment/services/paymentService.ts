import { getCsrfToken } from "@/features/shared/api/csrf";
import type { Payment, Refund } from "../types/payment";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const buildApiUrl = (path: string) => `${API_BASE_URL}${path}`;

async function parseData<T>(res: Response, fallbackMessage: string): Promise<T> {
  const body: { success?: boolean; data?: T; message?: string } = await res.json().catch(() => ({}));
  if (!res.ok || body.success === false || body.data === undefined) {
    throw new Error(body.message ?? fallbackMessage);
  }
  return body.data;
}

export const paymentService = {
  async getMyPayments(signal?: AbortSignal): Promise<Payment[]> {
    const res = await fetch(buildApiUrl("/api/v1/payments"), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<Payment[]>(res, "결제 내역을 불러오지 못했습니다.");
  },

  async getPayment(orderId: string, signal?: AbortSignal): Promise<Payment> {
    const res = await fetch(buildApiUrl(`/api/v1/payments/${orderId}`), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<Payment>(res, "결제 정보를 불러오지 못했습니다.");
  },

  async requestRefund(paymentId: string, reason: string): Promise<Refund> {
    const csrf = await getCsrfToken();
    const res = await fetch(buildApiUrl(`/api/v1/payments/${paymentId}/refunds`), {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      body: JSON.stringify({ reason }),
    });
    return parseData<Refund>(res, "환불 신청에 실패했습니다.");
  },
};
