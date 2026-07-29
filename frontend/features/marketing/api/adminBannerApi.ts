import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminBannerService } from "../services/adminBannerService";
import type { BannerSlot, MarketerBannerAd } from "../types/marketerBanner";

const adminBannerApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAdminAllAds: build.query<MarketerBannerAd[], void>({
      queryFn: (_arg, api) => queryResult(adminBannerService.getAllAds(api.signal)),
      providesTags: ["MarketerAd"],
    }),

    getAdminSlots: build.query<BannerSlot[], void>({
      queryFn: (_arg, api) => queryResult(adminBannerService.getSlots(api.signal)),
      providesTags: ["BannerSlot"],
    }),

    createSlot: build.mutation<BannerSlot, { name: string; maxCount: number }>({
      queryFn: (cmd) => queryResult(adminBannerService.createSlot(cmd)),
      invalidatesTags: ["BannerSlot"],
    }),

    approveAd: build.mutation<MarketerBannerAd, string>({
      queryFn: (id) => queryResult(adminBannerService.approveAd(id)),
      invalidatesTags: ["MarketerAd"],
    }),

    rejectAd: build.mutation<MarketerBannerAd, string>({
      queryFn: (id) => queryResult(adminBannerService.rejectAd(id)),
      invalidatesTags: ["MarketerAd"],
    }),

    deleteAd: build.mutation<void, string>({
      queryFn: (id) => queryResult(adminBannerService.deleteAd(id)),
      invalidatesTags: ["MarketerAd"],
    }),
  }),
});

export const {
  useGetAdminAllAdsQuery,
  useGetAdminSlotsQuery,
  useCreateSlotMutation,
  useApproveAdMutation,
  useRejectAdMutation,
  useDeleteAdMutation,
} = adminBannerApi;
