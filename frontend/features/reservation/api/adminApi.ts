import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminService } from "../services/adminService";
import type { Attendee, CheckinResponse, EventReservationSummary } from "../types/admin";

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
    checkin: build.mutation<CheckinResponse, string>({
      queryFn: (qrCodeHash) => queryResult(adminService.checkin(qrCodeHash)),
      invalidatesTags: ["Reservation"],
    }),
  }),
});

export const { useGetEventAttendeesQuery, useGetEventSummaryQuery, useCheckinMutation } = adminApi;
