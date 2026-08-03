import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { managerEventService } from "../services/managerEventService";
import type { EventListItem, AdminEventStatus } from "../types/eventList";
import type { CreateEventPayload, EventSummary } from "../types/eventCreate";

const managerEventApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getManagerEvents: build.query<EventListItem[], AdminEventStatus | undefined>({
      queryFn: (status, api) => queryResult(managerEventService.getManagerEvents(status, api.signal)),
      providesTags: (result) =>
        result
          ? [...result.map((event) => ({ type: "Event" as const, id: event.id })), { type: "Event" as const, id: "LIST" }]
          : [{ type: "Event" as const, id: "LIST" }],
    }),

    createManagerEvent: build.mutation<EventSummary, CreateEventPayload>({
      queryFn: (payload) => queryResult(managerEventService.createDraftEvent(payload)),
      invalidatesTags: [{ type: "Event", id: "LIST" }],
    }),

    deleteManagerEvent: build.mutation<void, string>({
      queryFn: (eventId) => queryResult(managerEventService.deleteManagerEvent(eventId)),
      invalidatesTags: (_result, _error, eventId) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),
  }),
});

export const { useGetManagerEventsQuery, useCreateManagerEventMutation, useDeleteManagerEventMutation } =
  managerEventApi;