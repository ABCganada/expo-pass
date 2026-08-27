import type { LogLevelSummary, LogSearchParams, LogSearchResult } from "../types/log";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const LOG_BASE = `${API_BASE_URL}/api/v1/monitoring/logs`;
const TRACE_ID_PATTERN = /^[0-9a-fA-F]{32}$/;

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

async function parseData<T>(response: Response): Promise<T> {
  const body = await response.json() as ApiResponse<T>;
  if (!response.ok || !body.success || body.data === undefined) {
    throw new Error(body.message ?? "로그를 불러오지 못했습니다.");
  }
  return body.data;
}

export const logService = {
  search: (params: LogSearchParams, signal?: AbortSignal): Promise<LogSearchResult> => {
    const searchText = params.searchText.trim();
    const query = new URLSearchParams({
      rangeMinutes: String(params.rangeMinutes),
      level: params.level,
      podName: params.podName,
      keyword: TRACE_ID_PATTERN.test(searchText) ? "" : searchText,
      traceId: TRACE_ID_PATTERN.test(searchText) ? searchText : "",
      limit: String(params.limit),
      cursor: params.cursor ?? "",
    });
    return fetch(`${LOG_BASE}?${query}`, { credentials: "include", signal })
      .then((response) => parseData<LogSearchResult>(response));
  },
  summary: (
    params: Pick<LogSearchParams, "rangeMinutes" | "podName">,
    signal?: AbortSignal,
  ): Promise<LogLevelSummary> => {
    const query = new URLSearchParams({
      rangeMinutes: String(params.rangeMinutes),
      podName: params.podName,
    });
    return fetch(`${LOG_BASE}/summary?${query}`, { credentials: "include", signal })
      .then((response) => parseData<LogLevelSummary>(response));
  },
};
