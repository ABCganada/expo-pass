import type { Payment } from "../types/payment";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const buildApiUrl = (path: string) => `${API_BASE_URL}${path}`;

export const paymentService = {
  async getMyPayments(signal?: AbortSignal): Promise<Payment[]> {
    const res = await fetch(buildApiUrl("/api/v1/payments"), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    if (!res.ok) throw new Error("결제 내역을 불러오지 못했습니다.");
    const body: { data: Payment[] } = await res.json();
    return body.data;
  },

  async getPayment(orderId: string, signal?: AbortSignal): Promise<Payment> {
    const res = await fetch(buildApiUrl(`/api/v1/payments/${orderId}`), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    if (!res.ok) throw new Error("결제 정보를 불러오지 못했습니다.");
    const body: { data: Payment } = await res.json();
    return body.data;
  },
};
