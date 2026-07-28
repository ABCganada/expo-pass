import { baseApi } from "@/features/store/api/baseApi";
import { queryResult } from "@/features/store/api/queryError";
import { memberService } from "../services/memberService";
import type { AdminMember, AdminMemberPage, MemberQuery, UpdateRolesCommand } from "../types/member";

export const memberApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getMembers: build.query<AdminMemberPage, MemberQuery>({
      queryFn: (query, api) => queryResult(memberService.findMembers(query, api.signal)),
      providesTags: [{ type: "AdminMember", id: "LIST" }],
    }),
    updateMemberRoles: build.mutation<AdminMember, UpdateRolesCommand>({
      queryFn: ({ id, roles }) => queryResult(memberService.updateRoles(id, roles)),
      invalidatesTags: [{ type: "AdminMember", id: "LIST" }],
    }),
  }),
});

export const { useGetMembersQuery, useUpdateMemberRolesMutation } = memberApi;
