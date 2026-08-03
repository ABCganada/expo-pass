import type { Settlement, SettlementPayment } from "../types/settlement";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(
  /\/+$/,
  "",
);
const BASE = `${API_BASE_URL}/api/v1/manager/payments/settlements`;

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
// 브랜치 작업 끝나면 반드시 제거할 것.
const MOCK_SETTLEMENTS: Settlement[] = [
  {
    id: "1",
    eventId: "101",
    totalSales: 45_200_000,
    commissionRate: 5.0,
    commissionAmount: 2_260_000,
    netAmount: 42_940_000,
    status: "COMPLETED",
    settledAt: "2026-07-20T10:00:00+09:00",
    createdAt: "2026-07-20T10:00:00+09:00",
  },
  {
    id: "2",
    eventId: "102",
    totalSales: 12_850_000,
    commissionRate: 5.0,
    commissionAmount: 642_500,
    netAmount: 12_207_500,
    status: "COMPLETED",
    settledAt: "2026-07-15T09:30:00+09:00",
    createdAt: "2026-07-15T09:30:00+09:00",
  },
  {
    id: "3",
    eventId: "104",
    totalSales: 3_400_000,
    commissionRate: 5.0,
    commissionAmount: 170_000,
    netAmount: 3_230_000,
    status: "COMPLETED",
    settledAt: "2026-06-30T18:00:00+09:00",
    createdAt: "2026-06-30T18:00:00+09:00",
  },
];

const MOCK_SETTLEMENT_PAYMENTS: Record<string, SettlementPayment[]> = {
  "1": [
    {
      id: "9001",
      orderId: "order_9001",
      amount: 45_000,
      refundAmount: 0,
      method: "카드",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9001",
      paidAt: "2026-07-10T13:20:00+09:00",
      createdAt: "2026-07-10T13:20:00+09:00",
    },
    {
      id: "9002",
      orderId: "order_9002",
      amount: 90_000,
      refundAmount: 20_000,
      method: "카드",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9002",
      paidAt: "2026-07-11T09:05:00+09:00",
      createdAt: "2026-07-11T09:05:00+09:00",
    },
    {
      id: "9003",
      orderId: "order_9003",
      amount: 45_000,
      refundAmount: 0,
      method: "간편결제",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9003",
      paidAt: "2026-07-12T20:41:00+09:00",
      createdAt: "2026-07-12T20:41:00+09:00",
    },
  ],
  "2": [
    {
      id: "9101",
      orderId: "order_9101",
      amount: 12_850_000,
      refundAmount: 0,
      method: "카드",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9101",
      paidAt: "2026-07-05T11:00:00+09:00",
      createdAt: "2026-07-05T11:00:00+09:00",
    },
  ],
  "3": [
    {
      id: "9201",
      orderId: "order_9201",
      amount: 1_700_000,
      refundAmount: 0,
      method: "카드",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9201",
      paidAt: "2026-06-20T15:30:00+09:00",
      createdAt: "2026-06-20T15:30:00+09:00",
    },
    {
      id: "9202",
      orderId: "order_9202",
      amount: 1_700_000,
      refundAmount: 1_700_000,
      method: "간편결제",
      status: "COMPLETED",
      pgProvider: "TOSS",
      pgTransactionId: "toss_tx_9202",
      paidAt: "2026-06-22T18:10:00+09:00",
      createdAt: "2026-06-22T18:10:00+09:00",
    },
  ],
};

export const settlementService = {
  async getSettlements(signal?: AbortSignal): Promise<Settlement[]> {
    return MOCK_SETTLEMENTS; // TODO(mock): remove before push

    const res = await fetch(BASE, {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<Settlement[]>(res, "정산 목록을 불러오지 못했습니다.");
  },

  async getSettlementDetail(
    settlementId: string,
    signal?: AbortSignal,
  ): Promise<Settlement> {
    return (
      MOCK_SETTLEMENTS.find((settlement) => settlement.id === settlementId) ??
      MOCK_SETTLEMENTS[0]
    ); // TODO(mock): remove before push

    const res = await fetch(`${BASE}/${settlementId}`, {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<Settlement>(res, "정산 상세 정보를 불러오지 못했습니다.");
  },

  async getSettlementPayments(
    settlementId: string,
    signal?: AbortSignal,
  ): Promise<SettlementPayment[]> {
    return MOCK_SETTLEMENT_PAYMENTS[settlementId] ?? []; // TODO(mock): remove before push

    const res = await fetch(`${BASE}/${settlementId}/payments`, {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<SettlementPayment[]>(
      res,
      "정산 결제 내역을 불러오지 못했습니다.",
    );
  },
};
