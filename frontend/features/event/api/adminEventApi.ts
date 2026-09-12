import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminEventService } from "../services/adminEventService";
import type { AdminEventListItem, AdminEventStatus } from "../types/adminEvent";

const adminEventApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAdminEvents: build.query<AdminEventListItem[], AdminEventStatus | undefined>({
      queryFn: (status, api) => queryResult(adminEventService.getAdminEvents(status, api.signal)),
      providesTags: (result) =>
        result
          ? [...result.map((event) => ({ type: "Event" as const, id: event.id })), { type: "Event" as const, id: "LIST" }]
          : [{ type: "Event" as const, id: "LIST" }],
    }),
  }),
});

export const { useGetAdminEventsQuery } = adminEventApi;