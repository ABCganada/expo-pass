import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { paymentService } from "../services/paymentService";
import type {
  Payment,
  PaymentConfirmRequest,
  PaymentFailRequest,
  Refund,
} from "../types/payment";

const paymentApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyPayments: build.query<Payment[], void>({
      queryFn: (_arg, api) =>
        queryResult(paymentService.getMyPayments(api.signal)),
      providesTags: ["Payment"],
    }),
    getPayment: build.query<Payment, string>({
      queryFn: (orderId, api) =>
        queryResult(paymentService.getPayment(orderId, api.signal)),
      providesTags: ["Payment"],
    }),
    requestRefund: build.mutation<
      Refund,
      { paymentId: string; reason: string }
    >({
      queryFn: ({ paymentId, reason }) =>
        queryResult(paymentService.requestRefund(paymentId, reason)),
      invalidatesTags: ["Payment", "Reservation"],
    }),
    confirmPayment: build.mutation<
      Payment,
      { orderId: string; request: PaymentConfirmRequest }
    >({
      queryFn: ({ orderId, request }) =>
        queryResult(paymentService.confirmPayment(orderId, request)),
      invalidatesTags: ["Payment", "Reservation"],
    }),
    failPayment: build.mutation<
      void,
      { orderId: string; request: PaymentFailRequest }
    >({
      queryFn: ({ orderId, request }) =>
        queryResult(paymentService.failPayment(orderId, request)),
      invalidatesTags: ["Payment", "Reservation"],
    }),
  }),
  overrideExisting: true,
});

export const {
  useGetMyPaymentsQuery,
  useGetPaymentQuery,
  useRequestRefundMutation,
  useConfirmPaymentMutation,
  useFailPaymentMutation,
} = paymentApi;
