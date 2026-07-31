import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { eventService } from "../services/eventService";
import type { 
    EventCategory, 
    EventListItem 
} from "../types/event";

const eventApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getEventCategories: build.query<EventCategory[], void>({
      queryFn: (_arg, api) => queryResult(eventService.getCategories(api.signal)),
      providesTags: ["EventCategory"],
    }),
    getEvents: build.query<EventListItem[], string | undefined>({
      queryFn: (categoryId, api) => queryResult(eventService.getEvents(categoryId, api.signal)),
      providesTags: ["Event"],
    }),
  }),
});

export const { useGetEventCategoriesQuery, useGetEventsQuery } = eventApi;