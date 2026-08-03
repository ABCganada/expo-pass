import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminEventDetailService } from "../services/adminEventDetailService";
import type { EventManagementDetail, EventTicket, CreateTicketPayload, UpdateTicketPayload } from "../types/eventManagementDetail";
import type { EventSummary } from "../types/eventCreate";

const adminEventDetailApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAdminEventDetail: build.query<EventManagementDetail, string>({
      queryFn: (eventId, api) => queryResult(adminEventDetailService.getAdminEventDetail(eventId, api.signal)),
      providesTags: (_result, _error, eventId) => [{ type: "Event", id: eventId }],
    }),

    publishAdminEvent: build.mutation<EventSummary, string>({
      queryFn: (eventId) => queryResult(adminEventDetailService.publishEvent(eventId)),
      invalidatesTags: (_result, _error, eventId) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    cancelAdminEvent: build.mutation<EventSummary, string>({
      queryFn: (eventId) => queryResult(adminEventDetailService.cancelAdminEvent(eventId)),
      invalidatesTags: (_result, _error, eventId) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    createAdminEventTicket: build.mutation<EventTicket, { eventId: string; payload: CreateTicketPayload }>({
      queryFn: ({ eventId, payload }) => queryResult(adminEventDetailService.createTicket(eventId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    updateAdminEventTicket: build.mutation<
      EventTicket,
      { eventId: string; ticketId: string; payload: UpdateTicketPayload }
    >({
      queryFn: ({ eventId, ticketId, payload }) =>
        queryResult(adminEventDetailService.updateTicket(eventId, ticketId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    deleteAdminEventTicket: build.mutation<void, { eventId: string; ticketId: string }>({
      queryFn: ({ eventId, ticketId }) => queryResult(adminEventDetailService.deleteTicket(eventId, ticketId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),
  }),
});

export const {
  useGetAdminEventDetailQuery,
  usePublishAdminEventMutation,
  useCancelAdminEventMutation,
  useCreateAdminEventTicketMutation,
  useUpdateAdminEventTicketMutation,
  useDeleteAdminEventTicketMutation,
} = adminEventDetailApi;