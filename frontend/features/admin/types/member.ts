export const ADMIN_ROLES = ["ADMIN", "MANAGER", "USER", "DEVELOPER"] as const;
export type AdminRole = (typeof ADMIN_ROLES)[number];

export const MEMBER_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export type MemberStatus = (typeof MEMBER_STATUSES)[number];

export interface AdminMember {
  id: string;
  email: string;
  name: string;
  status: MemberStatus;
  roles: AdminRole[];
}

export interface AdminMemberPage {
  members: AdminMember[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MemberQuery {
  query: string;
  page: number;
  size: number;
}

export interface UpdateRolesCommand {
  id: string;
  roles: AdminRole[];
}
