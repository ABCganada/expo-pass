import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminEventDetailService } from "../services/adminEventDetailService";
import type { AdminEventStatus } from "../types/adminEvent";
import type {
  AdminEventContentItem,
  AdminEventDetail,
  AdminEventImageItem,
  AdminEventTicket,
  CreateTicketPayload,
  EventContentType,
  EventImageType,
  UpdateEventPayload,
  UpdateTicketPayload,
} from "../types/adminEventDetail";

const adminEventDetailApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAdminEventDetail: build.query<AdminEventDetail, string>({
      queryFn: (eventId, api) => queryResult(adminEventDetailService.getEventDetail(eventId, api.signal)),
      providesTags: (_result, _error, eventId) => [{ type: "Event", id: eventId }],
    }),

    updateAdminEvent: build.mutation<void, { eventId: string; payload: UpdateEventPayload }>({
      queryFn: ({ eventId, payload }) => queryResult(adminEventDetailService.updateEvent(eventId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    changeAdminEventStatus: build.mutation<void, { eventId: string; status: AdminEventStatus }>({
      queryFn: ({ eventId, status }) => queryResult(adminEventDetailService.changeStatus(eventId, status)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    upsertAdminEventContents: build.mutation<
      AdminEventContentItem[],
      { eventId: string; contents: Partial<Record<EventContentType, string>> }
    >({
      queryFn: ({ eventId, contents }) => queryResult(adminEventDetailService.upsertContents(eventId, contents)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    uploadAdminEventImage: build.mutation<
      AdminEventImageItem,
      { eventId: string; file: File; imageType: EventImageType }
    >({
      queryFn: ({ eventId, file, imageType }) =>
        queryResult(adminEventDetailService.uploadImage(eventId, file, imageType)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    deleteAdminEventImage: build.mutation<void, { eventId: string; imageId: string }>({
      queryFn: ({ eventId, imageId }) => queryResult(adminEventDetailService.deleteImage(eventId, imageId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    createAdminEventTicket: build.mutation<AdminEventTicket, { eventId: string; payload: CreateTicketPayload }>({
      queryFn: ({ eventId, payload }) => queryResult(adminEventDetailService.createTicket(eventId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    updateAdminEventTicket: build.mutation<
      AdminEventTicket,
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
  useUpdateAdminEventMutation,
  useChangeAdminEventStatusMutation,
  useUpsertAdminEventContentsMutation,
  useUploadAdminEventImageMutation,
  useDeleteAdminEventImageMutation,
  useCreateAdminEventTicketMutation,
  useUpdateAdminEventTicketMutation,
  useDeleteAdminEventTicketMutation,
} = adminEventDetailApi;