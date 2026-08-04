import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminService } from "../services/adminService";
import type { Attendee, CheckinProgress, CheckinResponse, DailyReservationCount, EventReservationSummary } from "../types/admin";

const adminApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getEventAttendees: build.query<Attendee[], string>({
      queryFn: (eventId, api) => queryResult(adminService.getEventAttendees(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    getEventSummary: build.query<EventReservationSummary, string>({
      queryFn: (eventId, api) => queryResult(adminService.getEventSummary(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    getEventSummaryForAdmin: build.query<EventReservationSummary, string>({
      queryFn: (eventId, api) => queryResult(adminService.getEventSummaryForAdmin(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    getCheckinProgress: build.query<CheckinProgress, string>({
      queryFn: (eventId, api) => queryResult(adminService.getCheckinProgress(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    getDailyReservationCounts: build.query<DailyReservationCount[], string>({
      queryFn: (eventId, api) => queryResult(adminService.getDailyReservationCounts(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    getDailyReservationCountsForAdmin: build.query<DailyReservationCount[], string>({
      queryFn: (eventId, api) => queryResult(adminService.getDailyReservationCountsForAdmin(eventId, api.signal)),
      providesTags: ["Reservation"],
    }),
    checkin: build.mutation<CheckinResponse, string>({
      queryFn: (qrCodeHash) => queryResult(adminService.checkin(qrCodeHash)),
      invalidatesTags: ["Reservation"],
    }),
  }),
});

export const {
  useGetEventAttendeesQuery,
  useGetEventSummaryQuery,
  useGetEventSummaryForAdminQuery,
  useGetCheckinProgressQuery,
  useGetDailyReservationCountsQuery,
  useGetDailyReservationCountsForAdminQuery,
  useCheckinMutation,
} = adminApi;
