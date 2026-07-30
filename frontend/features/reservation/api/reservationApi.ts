import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { reservationService } from "../services/reservationService";
import type {
  CreateOrderItemInput,
  OrderDetail,
  OrderSummary,
  QrTicket,
  WaitingStatusResult,
  WaitingTicketResult,
} from "../types/reservation";

const reservationApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyOrders: build.query<OrderSummary[], void>({
      queryFn: (_arg, api) => queryResult(reservationService.getMyOrders(api.signal)),
      providesTags: ["Reservation"],
    }),
    getMyQrTickets: build.query<QrTicket[], void>({
      queryFn: (_arg, api) => queryResult(reservationService.getMyQrTickets(api.signal)),
      providesTags: ["Reservation"],
    }),
    enterWaitingRoom: build.mutation<WaitingTicketResult, string>({
      queryFn: (eventId) => queryResult(reservationService.enterWaitingRoom(eventId)),
    }),
    getWaitingRoomStatus: build.query<WaitingStatusResult, string>({
      queryFn: (eventId, api) => queryResult(reservationService.getWaitingRoomStatus(eventId, api.signal)),
    }),
    createReservationOrder: build.mutation<OrderDetail, { eventId: string; items: CreateOrderItemInput[] }>({
      queryFn: ({ eventId, items }) => queryResult(reservationService.createOrder(eventId, items)),
    }),
  }),
});

export const {
  useGetMyOrdersQuery,
  useGetMyQrTicketsQuery,
  useEnterWaitingRoomMutation,
  useGetWaitingRoomStatusQuery,
  useCreateReservationOrderMutation,
} = reservationApi;
