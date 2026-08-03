import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { eventCreateService } from "../services/eventCreateService";
import type { CreateEventPayload, EventSummary } from "../types/eventCreate";

const eventCreateApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    createDraftEvent: build.mutation<EventSummary, CreateEventPayload>({
      queryFn: (payload) => queryResult(eventCreateService.createDraftEvent(payload)),
      invalidatesTags: [{ type: "Event", id: "LIST" }],
    }),
  }),
});

export const { useCreateDraftEventMutation } = eventCreateApi;