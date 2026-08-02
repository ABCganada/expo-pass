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
      providesTags: (result) =>
        result
          ? [...result.map((event) => ({ type: "Event" as const, id: event.id })), { type: "Event" as const, id: "LIST" }]
          : [{ type: "Event" as const, id: "LIST" }],
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
      async onQueryStarted(eventId, { dispatch, getState, queryFulfilled }) {
        const patch = dispatch( // Optimistic Update : RTK Qeury 캐시 수정
          eventApi.util.updateQueryData("getMyBookmarks", undefined, (draft) => {
            const index = draft.findIndex((bookmark) => bookmark.id === eventId);
            if (index >= 0) { // 이미 북마크 -> 제거
              draft.splice(index, 1);
              return;
            }
            const detail = eventApi.endpoints.getEventDetail.select(eventId)(getState()).data;
            if (detail?.startDate && detail.endDate) {
              draft.push({ // 캐시에 추가
                id: detail.id,
                title: detail.title,
                categoryName: detail.categoryName,
                startDate: detail.startDate,
                endDate: detail.endDate,
                phase: detail.phase,
              });
            }
          }),
        );
        try {
          await queryFulfilled;
        } catch {
          patch.undo();
        }
      },
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