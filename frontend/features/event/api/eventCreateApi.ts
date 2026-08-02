import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { eventCreateService } from "../services/eventCreateService";
import type { CreateEventPayload, CreatedEvent } from "../types/eventCreate";

const eventCreateApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    createDraftEvent: build.mutation<CreatedEvent, CreateEventPayload>({
      queryFn: (payload) => queryResult(eventCreateService.createDraftEvent(payload)),
      invalidatesTags: [{ type: "Event", id: "LIST" }],
    }),

    changeEventManager: build.mutation<CreatedEvent, { eventId: string; managerId: string }>({
      queryFn: ({ eventId, managerId }) => queryResult(eventCreateService.changeManager(eventId, managerId)),
      invalidatesTags: (_result, _error, { eventId }) => [
        { type: "Event", id: eventId },
        { type: "Event", id: "LIST" },
      ],
    }),
  }),
});

export const { useCreateDraftEventMutation, useChangeEventManagerMutation } = eventCreateApi;