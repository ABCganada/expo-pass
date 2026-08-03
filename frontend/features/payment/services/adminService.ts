import type {
  DashboardSummary,
  PaymentLogDetail,
  PaymentLogPage,
} from "../types/admin";

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

export const adminService = {
  async getDashboard(signal?: AbortSignal): Promise<DashboardSummary> {
    const res = await fetch(buildApiUrl("/api/v1/admin/payments/dashboard"), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<DashboardSummary>(
      res,
      "매출 대시보드를 불러오지 못했습니다.",
    );
  },

  async getPaymentLogs(
    page: number,
    size: number,
    signal?: AbortSignal,
  ): Promise<PaymentLogPage> {
    const res = await fetch(
      buildApiUrl(`/api/v1/admin/payments/logs?page=${page}&size=${size}`),
      {
        credentials: "include",
        cache: "no-store",
        signal,
      },
    );
    return parseData<PaymentLogPage>(
      res,
      "결제 로그 목록을 불러오지 못했습니다.",
    );
  },

  async getPaymentLogDetail(
    id: string,
    signal?: AbortSignal,
  ): Promise<PaymentLogDetail> {
    const res = await fetch(buildApiUrl(`/api/v1/admin/payments/logs/${id}`), {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<PaymentLogDetail>(
      res,
      "결제 로그 상세 정보를 불러오지 못했습니다.",
    );
  },
};
