import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { bannerService } from "../services/bannerService";
import type { BannerAd } from "../types/banner";

const bannerApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getActiveBanners: build.query<BannerAd[], void>({
      queryFn: (_arg, api) =>
        queryResult(bannerService.getActiveBanners(api.signal)),
      providesTags: ["Banner"],
    }),
  }),
});

export const { useGetActiveBannersQuery } = bannerApi;
