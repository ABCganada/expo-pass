import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { paymentService } from "../services/paymentService";
import type { Payment, Refund } from "../types/payment";

const paymentApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyPayments: build.query<Payment[], void>({
      queryFn: (_arg, api) => queryResult(paymentService.getMyPayments(api.signal)),
      providesTags: ["Payment"],
    }),
    getPayment: build.query<Payment, string>({
      queryFn: (orderId, api) => queryResult(paymentService.getPayment(orderId, api.signal)),
      providesTags: ["Payment"],
    }),
    requestRefund: build.mutation<Refund, { paymentId: string; reason: string }>({
      queryFn: ({ paymentId, reason }) => queryResult(paymentService.requestRefund(paymentId, reason)),
      invalidatesTags: ["Payment", "Reservation"],
    }),
  }),
  overrideExisting: true,
});

export const { useGetMyPaymentsQuery, useGetPaymentQuery, useRequestRefundMutation } = paymentApi;
