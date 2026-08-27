import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminEventService } from "../services/adminEventService";
import type { EventListItem, AdminEventStatus } from "../types/eventList";
import type { EventSummary } from "../types/eventCreate";

const adminEventApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAdminEvents: build.query<EventListItem[], AdminEventStatus | undefined>({
      queryFn: (status, api) => queryResult(adminEventService.getAdminEvents(status, api.signal)),
      providesTags: (result) =>
        result
          ? [...result.map((event) => ({ type: "Event" as const, id: event.id })), { type: "Event" as const, id: "LIST" }]
          : [{ type: "Event" as const, id: "LIST" }],
    }),

    deleteAdminEvent: build.mutation<void, string>({
      queryFn: (eventId) => queryResult(adminEventService.deleteAdminEvent(eventId)),
      invalidatesTags: (_result, _error, eventId) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),

    changeAdminEventManager: build.mutation<EventSummary, { eventId: string; managerId: string }>({
      queryFn: ({ eventId, managerId }) => queryResult(adminEventService.changeManager(eventId, managerId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),
  }),
});

export const {
  useGetAdminEventsQuery,
  useDeleteAdminEventMutation,
  useChangeAdminEventManagerMutation,
} = adminEventApi;