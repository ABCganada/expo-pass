import type { MetricSnapshot, MetricTrendDashboard } from "../types/metric";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const METRIC_BASE = `${API_BASE_URL}/api/v1/monitoring/metrics`;

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

async function parseData<T>(response: Response): Promise<T> {
  const body = await response.json() as ApiResponse<T>;
  if (!response.ok || !body.success || body.data === undefined) {
    throw new Error(body.message ?? "메트릭을 불러오지 못했습니다.");
  }
  return body.data;
}

export const monitoringService = {
  getDailyTrends: (signal?: AbortSignal): Promise<MetricTrendDashboard> =>
    fetch(`${METRIC_BASE}/trends/daily`, { credentials: "include", signal })
      .then((response) => parseData<MetricTrendDashboard>(response)),
  getCurrentMetrics: (signal?: AbortSignal): Promise<MetricSnapshot> =>
    fetch(`${METRIC_BASE}/current`, { credentials: "include", signal })
      .then((response) => parseData<MetricSnapshot>(response)),
};
