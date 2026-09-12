import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { reservationService } from "../services/reservationService";
import type {
  CreateOrderItemInput,
  MyOrdersQuery,
  OrderDetail,
  OrdersPage,
  QrTicket,
} from "../types/reservation";

const reservationApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyOrders: build.query<OrdersPage, MyOrdersQuery>({
      queryFn: (query, api) => queryResult(reservationService.getMyOrders(query, api.signal)),
      providesTags: ["Reservation"],
    }),
    getMyQrTickets: build.query<QrTicket[], void>({
      queryFn: (_arg, api) => queryResult(reservationService.getMyQrTickets(api.signal)),
      providesTags: ["Reservation"],
    }),
    getOrder: build.query<OrderDetail, string>({
      queryFn: (orderId, api) => queryResult(reservationService.getOrder(orderId, api.signal)),
      providesTags: ["Reservation"],
    }),
    createReservationOrder: build.mutation<OrderDetail, { eventId: string; items: CreateOrderItemInput[] }>({
      queryFn: ({ eventId, items }) => queryResult(reservationService.createOrder(eventId, items)),
    }),
  }),
});

export const {
  useGetMyOrdersQuery,
  useGetMyQrTicketsQuery,
  useGetOrderQuery,
  useCreateReservationOrderMutation,
} = reservationApi;
