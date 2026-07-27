import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { authService } from "../services/authService";
import type { CurrentUser } from "../types/auth";

const authApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getCurrentUser: build.query<CurrentUser | null, void>({
      queryFn: (_argument, api) =>
        queryResult(authService.getCurrentUser(api.signal)),
      providesTags: ["Auth"],
    }),
  }),
});

export const { useGetCurrentUserQuery } = authApi;
