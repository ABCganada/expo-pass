import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { traceService } from "../services/traceService";
import type { TraceDetail, TraceSearchParams, TraceSearchResult } from "../types/trace";

const traceApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    searchTraces: build.query<TraceSearchResult, TraceSearchParams>({
      queryFn: (params, api) => queryResult(traceService.search(params, api.signal)),
    }),
    getTraceDetail: build.query<TraceDetail, string>({
      queryFn: (traceId, api) => queryResult(traceService.getDetail(traceId, api.signal)),
    }),
  }),
});

export const { useLazySearchTracesQuery, useLazyGetTraceDetailQuery } = traceApi;
