import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { managerEventDetailService } from "../services/managerEventDetailService";
import type {
  EventContentItem,
  EventManagementDetail,
  EventImageItem,
  EventTicket,
  CreateTicketPayload,
  EventContentType,
  EventImageType,
  UpdateEventPayload,
  UpdateTicketPayload,
} from "../types/eventManagementDetail";
import type { EventSummary } from "../types/eventCreate";

const managerEventDetailApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getManagerEventDetail: build.query<EventManagementDetail, string>({
      queryFn: (eventId, api) => queryResult(managerEventDetailService.getManagerEventDetail(eventId, api.signal)),
      providesTags: (_result, _error, eventId) => [{ type: "Event", id: eventId }],
    }),

    updateManagerEvent: build.mutation<EventSummary, { eventId: string; payload: UpdateEventPayload }>({
      queryFn: ({ eventId, payload }) => queryResult(managerEventDetailService.updateEvent(eventId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    cancelManagerEvent: build.mutation<EventSummary, string>({
      queryFn: (eventId) => queryResult(managerEventDetailService.cancelManagerEvent(eventId)),
      invalidatesTags: (_result, _error, eventId) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    upsertManagerEventContents: build.mutation<
      EventContentItem[],
      { eventId: string; contents: Partial<Record<EventContentType, string>> }
    >({
      queryFn: ({ eventId, contents }) => queryResult(managerEventDetailService.upsertContents(eventId, contents)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    uploadManagerEventImage: build.mutation<
      EventImageItem,
      { eventId: string; file: File; imageType: EventImageType }
    >({
      queryFn: ({ eventId, file, imageType }) =>
        queryResult(managerEventDetailService.uploadImage(eventId, file, imageType)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    deleteManagerEventImage: build.mutation<void, { eventId: string; imageId: string }>({
      queryFn: ({ eventId, imageId }) => queryResult(managerEventDetailService.deleteImage(eventId, imageId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    createManagerEventTicket: build.mutation<EventTicket, { eventId: string; payload: CreateTicketPayload }>({
      queryFn: ({ eventId, payload }) => queryResult(managerEventDetailService.createTicket(eventId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    updateManagerEventTicket: build.mutation<
      EventTicket,
      { eventId: string; ticketId: string; payload: UpdateTicketPayload }
    >({
      queryFn: ({ eventId, ticketId, payload }) =>
        queryResult(managerEventDetailService.updateTicket(eventId, ticketId, payload)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    deleteManagerEventTicket: build.mutation<void, { eventId: string; ticketId: string }>({
      queryFn: ({ eventId, ticketId }) => queryResult(managerEventDetailService.deleteTicket(eventId, ticketId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),
  }),
});

export const {
  useGetManagerEventDetailQuery,
  useUpdateManagerEventMutation,
  useCancelManagerEventMutation,
  useUpsertManagerEventContentsMutation,
  useUploadManagerEventImageMutation,
  useDeleteManagerEventImageMutation,
  useCreateManagerEventTicketMutation,
  useUpdateManagerEventTicketMutation,
  useDeleteManagerEventTicketMutation,
} = managerEventDetailApi;