import type { Settlement } from "../types/settlement";

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

// TODO(mock): 아래 MOCK_SETTLEMENTS와 getSettlements의 조기 return은 디자인 확인용 임시 데이터다.
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
    const res = await fetch(`${BASE}/${settlementId}`, {
      credentials: "include",
      cache: "no-store",
      signal,
    });
    return parseData<Settlement>(res, "정산 상세 정보를 불러오지 못했습니다.");
  },
};
