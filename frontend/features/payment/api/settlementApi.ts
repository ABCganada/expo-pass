import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { settlementService } from "../services/settlementService";
import type { Settlement, SettlementPayment } from "../types/settlement";

const settlementApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getSettlements: build.query<Settlement[], void>({
      queryFn: (_arg, api) =>
        queryResult(settlementService.getSettlements(api.signal)),
      providesTags: ["Payment"],
    }),
    getSettlementDetail: build.query<Settlement, string>({
      queryFn: (settlementId, api) =>
        queryResult(
          settlementService.getSettlementDetail(settlementId, api.signal),
        ),
      providesTags: ["Payment"],
    }),
    getSettlementPayments: build.query<SettlementPayment[], string>({
      queryFn: (settlementId, api) =>
        queryResult(
          settlementService.getSettlementPayments(settlementId, api.signal),
        ),
      providesTags: ["Payment"],
    }),
  }),
  overrideExisting: true,
});

export const {
  useGetSettlementsQuery,
  useGetSettlementDetailQuery,
  useGetSettlementPaymentsQuery,
} = settlementApi;
