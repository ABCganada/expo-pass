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

export const settlementService = {
  async getSettlements(signal?: AbortSignal): Promise<Settlement[]> {
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
