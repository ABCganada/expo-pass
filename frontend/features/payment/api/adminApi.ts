import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminService } from "../services/adminService";
import type {
  DashboardSummary,
  PaymentLogDetail,
  PaymentLogPage,
} from "../types/admin";

const adminApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getPaymentDashboard: build.query<DashboardSummary, void>({
      queryFn: (_arg, api) =>
        queryResult(adminService.getDashboard(api.signal)),
      providesTags: ["Payment"],
    }),
    getPaymentLogs: build.query<PaymentLogPage, { page: number; size: number }>(
      {
        queryFn: ({ page, size }, api) =>
          queryResult(adminService.getPaymentLogs(page, size, api.signal)),
        providesTags: ["Payment"],
      },
    ),
    getPaymentLogDetail: build.query<PaymentLogDetail, string>({
      queryFn: (id, api) =>
        queryResult(adminService.getPaymentLogDetail(id, api.signal)),
      providesTags: ["Payment"],
    }),
  }),
});

export const {
  useGetPaymentDashboardQuery,
  useGetPaymentLogsQuery,
  useGetPaymentLogDetailQuery,
} = adminApi;
