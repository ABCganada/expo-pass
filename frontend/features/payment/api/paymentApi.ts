import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { paymentService } from "../services/paymentService";
import type { Payment } from "../types/payment";

const paymentApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyPayments: build.query<Payment[], void>({
      queryFn: (_arg, api) => queryResult(paymentService.getMyPayments(api.signal)),
      providesTags: ["Payment"],
    }),
  }),
});

export const { useGetMyPaymentsQuery } = paymentApi;
