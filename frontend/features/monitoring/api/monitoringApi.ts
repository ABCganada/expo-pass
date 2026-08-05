import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { monitoringService } from "../services/monitoringService";
import type { MetricSnapshot, MetricTrendDashboard } from "../types/metric";

const monitoringApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getDailyTrends: build.query<MetricTrendDashboard, void>({
      queryFn: (_arg, api) => queryResult(monitoringService.getDailyTrends(api.signal)),
    }),
    getCurrentMetrics: build.query<MetricSnapshot, void>({
      queryFn: (_arg, api) => queryResult(monitoringService.getCurrentMetrics(api.signal)),
    }),
  }),
});

export const { useLazyGetCurrentMetricsQuery, useLazyGetDailyTrendsQuery } = monitoringApi;
