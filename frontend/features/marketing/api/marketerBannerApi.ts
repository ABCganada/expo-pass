import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { marketerBannerService } from "../services/marketerBannerService";
import type {
  BannerAdStats,
  BannerSlotWithPolicies,
  MarketerBannerAd,
  RegisterAdCommand,
  UpdateAdCommand,
} from "../types/marketerBanner";

const marketerBannerApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMyAds: build.query<MarketerBannerAd[], void>({
      queryFn: (_arg, api) => queryResult(marketerBannerService.getMyAds(api.signal)),
      providesTags: ["MarketerAd"],
    }),

    getBannerSlotsWithPolicies: build.query<BannerSlotWithPolicies[], void>({
      queryFn: (_arg, api) => queryResult(marketerBannerService.getSlotsWithPolicies(api.signal)),
      providesTags: ["BannerSlot"],
    }),

    registerAd: build.mutation<MarketerBannerAd, RegisterAdCommand>({
      queryFn: (cmd) => queryResult(marketerBannerService.registerAd(cmd)),
      invalidatesTags: ["MarketerAd"],
    }),

    updateAd: build.mutation<MarketerBannerAd, { id: string } & UpdateAdCommand>({
      queryFn: ({ id, ...cmd }) => queryResult(marketerBannerService.updateAd(id, cmd)),
      invalidatesTags: ["MarketerAd"],
    }),

    getAdStats: build.query<BannerAdStats, { id: string; from: string; to: string }>({
      queryFn: ({ id, from, to }, api) =>
        queryResult(marketerBannerService.getStatsByDateRange(id, from, to, api.signal)),
    }),
  }),
});

export const {
  useGetMyAdsQuery,
  useGetBannerSlotsWithPoliciesQuery,
  useRegisterAdMutation,
  useUpdateAdMutation,
  useGetAdStatsQuery,
} = marketerBannerApi;
