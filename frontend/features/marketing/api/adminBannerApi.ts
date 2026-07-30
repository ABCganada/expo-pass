import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminBannerService } from "../services/adminBannerService";
import type { BannerPricingPolicy, BannerSlot, BannerSlotType, MarketerBannerAd } from "../types/marketerBanner";

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

    createSlot: build.mutation<BannerSlot, { name: string; maxCount: number; type: BannerSlotType }>({
      queryFn: (cmd) => queryResult(adminBannerService.createSlot(cmd)),
      invalidatesTags: ["BannerSlot"],
    }),

    updateSlot: build.mutation<BannerSlot, { id: string; name: string; maxCount: number; type: BannerSlotType }>({
      queryFn: ({ id, ...cmd }) => queryResult(adminBannerService.updateSlot(id, cmd)),
      invalidatesTags: ["BannerSlot"],
    }),

    deleteSlot: build.mutation<void, string>({
      queryFn: (id) => queryResult(adminBannerService.deleteSlot(id)),
      invalidatesTags: ["BannerSlot"],
    }),

    getPoliciesBySlot: build.query<BannerPricingPolicy[], string>({
      queryFn: (slotId, api) => queryResult(adminBannerService.getPoliciesBySlot(slotId, api.signal)),
      providesTags: (_result, _err, slotId) => [{ type: "BannerPolicy", id: slotId }],
    }),

    createPolicy: build.mutation<BannerPricingPolicy, { slotId: string; durationDays: number; price: number }>({
      queryFn: ({ slotId, ...cmd }) => queryResult(adminBannerService.createPolicy(slotId, cmd)),
      invalidatesTags: (_result, _err, { slotId }) => [{ type: "BannerPolicy", id: slotId }],
    }),

    deletePolicy: build.mutation<void, { slotId: string; policyId: string }>({
      queryFn: ({ slotId, policyId }) => queryResult(adminBannerService.deletePolicy(slotId, policyId)),
      invalidatesTags: (_result, _err, { slotId }) => [{ type: "BannerPolicy", id: slotId }],
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
  useUpdateSlotMutation,
  useDeleteSlotMutation,
  useGetPoliciesBySlotQuery,
  useCreatePolicyMutation,
  useDeletePolicyMutation,
  useApproveAdMutation,
  useRejectAdMutation,
  useDeleteAdMutation,
} = adminBannerApi;
