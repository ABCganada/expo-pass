import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { logService } from "../services/logService";
import type { LogLevelSummary, LogSearchParams, LogSearchResult } from "../types/log";

const logApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchLogs: build.query<LogSearchResult, LogSearchParams>({
      queryFn: (params, api) => queryResult(logService.search(params, api.signal)),
    }),
    getLogSummary: build.query<LogLevelSummary, Pick<LogSearchParams, "rangeMinutes" | "podName">>({
      queryFn: (params, api) => queryResult(logService.summary(params, api.signal)),
    }),
  }),
});

export const { useLazySearchLogsQuery, useLazyGetLogSummaryQuery } = logApi;
