import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { eventService } from "../services/eventService";
import type {
    BookmarkedEvent,
    EventCategory,
    EventDetail,
    EventListItem,
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
    getEventDetail: build.query<EventDetail, string>({
      queryFn: (eventId, api) => queryResult(eventService.getEventDetail(eventId, api.signal)),
      providesTags: (_result, _error, eventId) => [{ type: "Event", id: eventId }],
    }),
    getMyBookmarks: build.query<BookmarkedEvent[], void>({
      queryFn: (_arg, api) => queryResult(eventService.getMyBookmarks(api.signal)),
      providesTags: ["EventBookmark"],
    }),
    toggleBookmark: build.mutation<{ isBookmarked: boolean }, string>({
      queryFn: (eventId, api) => queryResult(eventService.toggleBookmark(eventId, api.signal)),
      invalidatesTags: ["EventBookmark"],
    }),
  }),
});

export const {
  useGetEventCategoriesQuery,
  useGetEventsQuery,
  useGetEventDetailQuery,
  useGetMyBookmarksQuery,
  useToggleBookmarkMutation,
} = eventApi;