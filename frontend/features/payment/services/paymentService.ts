import { getCsrfToken } from "@/features/shared/api/csrf";
import type {
  Payment,
  PaymentConfirmRequest,
  PaymentFailRequest,
  Refund,
} from "../types/payment";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(
  /\/+$/,
  "",
);
const buildApiUrl = (path: string) => `${API_BASE_URL}${path}`;

async function parseData<T>(
  res: Response,
  fallbackMessage: string,
): Promise<T> {
  const body: { success?: boolean; data?: T; message?: string } = await res
    .json()
    .catch(() => ({}));
  if (!res.ok || body.success === false || body.data === undefined) {
    throw new Error(body.message ?? fallbackMessage);
  }
  return body.data;
}

// data 없이 success/message만 오는 응답용 (예: 결제 실패 신고 — data가 null이면
// @JsonInclude(NON_NULL)에 의해 키 자체가 응답에서 빠지므로 parseData를 쓰면 안 된다).
async function parseVoid(
  res: Response,
  fallbackMessage: string,
): Promise<void> {
  const body: { success?: boolean; message?: string } = await res
    .json()
    .catch(() => ({}));
  if (!res.ok || body.success === false) {
    throw new Error(body.message ?? fallbackMessage);
  }
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
    const res = await fetch(
      buildApiUrl(`/api/v1/payments/${paymentId}/refunds`),
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ reason }),
      },
    );
    return parseData<Refund>(res, "환불 신청에 실패했습니다.");
  },

  async confirmPayment(
    orderId: string,
    request: PaymentConfirmRequest,
  ): Promise<Payment> {
    const csrf = await getCsrfToken();
    const res = await fetch(
      buildApiUrl(`/api/v1/payments/${orderId}/confirm`),
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(request),
      },
    );
    return parseData<Payment>(res, "결제 승인에 실패했습니다.");
  },

  async failPayment(
    orderId: string,
    request: PaymentFailRequest,
  ): Promise<void> {
    const csrf = await getCsrfToken();
    const res = await fetch(buildApiUrl(`/api/v1/payments/${orderId}/fail`), {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify(request),
    });
    return parseVoid(res, "결제 실패 신고에 실패했습니다.");
  },
};
