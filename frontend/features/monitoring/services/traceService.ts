import type { TraceDetail, TraceSearchParams, TraceSearchResult } from "../types/trace";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const TRACE_BASE = `${API_BASE_URL}/api/v1/monitoring/traces`;

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

async function parseData<T>(response: Response): Promise<T> {
  const body = await response.json() as ApiResponse<T>;
  if (!response.ok || !body.success || body.data === undefined) {
    throw new Error(body.message ?? "트레이스를 불러오지 못했습니다.");
  }
  return body.data;
}

export const traceService = {
  search: (params: TraceSearchParams, signal?: AbortSignal): Promise<TraceSearchResult> => {
    const query = new URLSearchParams({
      category: params.category,
      rangeMinutes: String(params.rangeMinutes),
      status: params.status,
      minDurationMs: String(params.minDurationMs),
      operation: params.operation,
      limit: String(params.limit),
    });
    return fetch(`${TRACE_BASE}?${query}`, { credentials: "include", signal })
      .then((response) => parseData<TraceSearchResult>(response));
  },
  getDetail: (traceId: string, signal?: AbortSignal): Promise<TraceDetail> =>
    fetch(`${TRACE_BASE}/${encodeURIComponent(traceId)}`, { credentials: "include", signal })
      .then((response) => parseData<TraceDetail>(response)),
};
