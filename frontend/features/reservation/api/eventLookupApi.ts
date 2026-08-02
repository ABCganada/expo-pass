import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { eventLookupService } from "../services/eventLookupService";
import type { AdminEventListItem, EventSummary, EventTicketOption } from "../types/eventLookup";

// Reservation 화면(예약 카드, 티켓 구매 위젯 등)이 Event의 공개 API를 조회 전용으로 쓰는 엔드포인트.
// Event 도메인의 프론트 기능을 대신 만드는 게 아니다.
const eventLookupApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getEventForDisplay: build.query<EventSummary, string>({
      queryFn: (eventId, api) => queryResult(eventLookupService.getEvent(eventId, api.signal)),
      providesTags: ["Event"],
    }),
    getEventTicketsForDisplay: build.query<EventTicketOption[], string>({
      queryFn: (eventId, api) => queryResult(eventLookupService.getEventTickets(eventId, api.signal)),
      providesTags: ["Event"],
    }),
    getAdminEventsForDisplay: build.query<AdminEventListItem[], void>({
      queryFn: (_arg, api) => queryResult(eventLookupService.getAdminEvents(api.signal)),
      providesTags: ["Event"],
    }),
  }),
});

export const {
  useGetEventForDisplayQuery,
  useLazyGetEventForDisplayQuery,
  useGetEventTicketsForDisplayQuery,
  useGetAdminEventsForDisplayQuery,
} = eventLookupApi;
