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

// TODO(mock): 아래 MOCK_* 상수와 각 함수 안의 조기 return은 디자인 확인용 임시 데이터다.
// 푸시 전에 반드시 제거할 것.
const MOCK_DASHBOARD: DashboardSummary = {
  totalSales: 128_450_000,
  totalCommissionAmount: 6_422_500,
  totalNetAmount: 122_027_500,
  settlementCount: 34,
};

const MOCK_LOGS: PaymentLogDetail[] = [
  {
    id: "1001",
    paymentKey: "tviva_9F3kQ2mZpL",
    action: "REQUEST",
    requestPayload: '{"orderId":"order_1001","amount":45000}',
    responsePayload: null,
    webhookTransmissionId: null,
    createdAt: "2026-08-03T09:12:00+09:00",
  },
  {
    id: "1002",
    paymentKey: "tviva_9F3kQ2mZpL",
    action: "APPROVE",
    requestPayload: '{"paymentKey":"tviva_9F3kQ2mZpL"}',
    responsePayload: '{"status":"DONE"}',
    webhookTransmissionId: null,
    createdAt: "2026-08-03T09:12:04+09:00",
  },
  {
    id: "1003",
    paymentKey: "tviva_7hR1sV0eXa",
    action: "PAYMENT_STATUS_CHANGED",
    requestPayload: null,
    responsePayload: '{"status":"DONE"}',
    webhookTransmissionId: "whk_2c9f1e8a-11",
    createdAt: "2026-08-03T08:47:00+09:00",
  },
  {
    id: "1004",
    paymentKey: "tviva_1kLwT4dPqe",
    action: "CANCEL",
    requestPayload: '{"cancelReason":"고객 요청"}',
    responsePayload: '{"status":"CANCELED"}',
    webhookTransmissionId: null,
    createdAt: "2026-08-02T22:03:00+09:00",
  },
  {
    id: "1005",
    paymentKey: "tviva_1kLwT4dPqe",
    action: "CANCEL_STATUS_CHANGED",
    requestPayload: null,
    responsePayload: '{"status":"CANCELED"}',
    webhookTransmissionId: "whk_2c9f1e8a-09",
    createdAt: "2026-08-02T22:03:05+09:00",
  },
  {
    id: "1006",
    paymentKey: "tviva_qX8zM6nYoV",
    action: "REFUND",
    requestPayload: '{"reason":"사용자 요청"}',
    responsePayload: '{"status":"REFUNDED"}',
    webhookTransmissionId: null,
    createdAt: "2026-08-02T19:31:00+09:00",
  },
  {
    id: "1007",
    paymentKey: "tviva_Zb2vN5wKri",
    action: "APPROVE",
    requestPayload: '{"paymentKey":"tviva_Zb2vN5wKri"}',
    responsePayload: '{"status":"DONE"}',
    webhookTransmissionId: null,
    createdAt: "2026-08-02T15:58:00+09:00",
  },
];

const MOCK_TOTAL_ELEMENTS = 52;

export const adminService = {
  async getDashboard(signal?: AbortSignal): Promise<DashboardSummary> {
    return MOCK_DASHBOARD; // TODO(mock): remove before push

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
    // TODO(mock): remove before push
    const logs = Array.from({ length: size }, (_, i) => {
      const template = MOCK_LOGS[(page * size + i) % MOCK_LOGS.length];
      return { ...template, id: `${page}-${i}` };
    });
    return {
      logs,
      page,
      size,
      totalElements: MOCK_TOTAL_ELEMENTS,
      totalPages: Math.ceil(MOCK_TOTAL_ELEMENTS / size),
    };

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
    return MOCK_LOGS.find((log) => log.id === id) ?? MOCK_LOGS[0]; // TODO(mock): remove before push

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
