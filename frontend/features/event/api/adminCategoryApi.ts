import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { adminCategoryService } from "../services/adminCategoryService";
import type { EventCategory } from "../types/event";

const adminCategoryApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getAllAdminCategories: build.query<EventCategory[], void>({
      queryFn: (_arg, api) => queryResult(adminCategoryService.getAllCategories(api.signal)),
      providesTags: ["EventCategory"],
    }),

    toggleCategoryActive: build.mutation<EventCategory, string>({
      queryFn: (categoryId) => queryResult(adminCategoryService.toggleActive(categoryId)),
      invalidatesTags: ["EventCategory"],
    }),
  }),
});

export const { useGetAllAdminCategoriesQuery, useToggleCategoryActiveMutation } = adminCategoryApi;