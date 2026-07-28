import { getCsrfToken } from "../../shared/api/csrf";
import {
  ADMIN_ROLES,
  MEMBER_STATUSES,
  type AdminMember,
  type AdminMemberPage,
  type AdminRole,
  type MemberQuery,
  type MemberStatus,
} from "../types/member";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isAdminRole(value: unknown): value is AdminRole {
  return typeof value === "string" && ADMIN_ROLES.some((role) => role === value);
}

function isMemberStatus(value: unknown): value is MemberStatus {
  return typeof value === "string" && MEMBER_STATUSES.some((status) => status === value);
}

function isAdminMember(value: unknown): value is AdminMember {
  return isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.email === "string" &&
    typeof value.name === "string" &&
    isMemberStatus(value.status) &&
    Array.isArray(value.roles) && value.roles.every(isAdminRole);
}

function isAdminMemberPage(value: unknown): value is AdminMemberPage {
  return isRecord(value) &&
    Array.isArray(value.members) && value.members.every(isAdminMember) &&
    typeof value.page === "number" &&
    typeof value.size === "number" &&
    typeof value.totalElements === "number" &&
    typeof value.totalPages === "number";
}

async function parseBody(response: Response): Promise<unknown> {
  return response.json().catch(() => null);
}

function errorMessage(body: unknown, status: number): string {
  return isRecord(body) && typeof body.message === "string"
    ? body.message
    : `회원 요청에 실패했습니다. (${status})`;
}

async function get<T>(
  path: string,
  isValid: (value: unknown) => value is T,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    cache: "no-store",
    signal,
  });
  const body = (await parseBody(response)) as ApiResponse<unknown> | null;
  if (!response.ok) throw new Error(errorMessage(body, response.status));
  if (!isRecord(body) || !isValid(body.data)) {
    throw new Error("회원 응답 형식이 올바르지 않습니다.");
  }
  return body.data;
}

async function put(path: string, payload: unknown): Promise<unknown> {
  const csrf = await getCsrfToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "PUT",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  const body = (await parseBody(response)) as ApiResponse<unknown> | null;
  if (!response.ok) throw new Error(errorMessage(body, response.status));
  return isRecord(body) ? body.data : null;
}

export const memberService = {
  async findMembers({ query, page, size }: MemberQuery, signal?: AbortSignal): Promise<AdminMemberPage> {
    const params = new URLSearchParams({
      query,
      page: String(page),
      size: String(size),
    });
    return get(`/api/v1/admin/members?${params.toString()}`, isAdminMemberPage, signal);
  },

  async updateRoles(id: string, roles: AdminRole[]): Promise<AdminMember> {
    const data = await put(`/api/v1/admin/members/${id}/roles`, { roles });
    if (!isAdminMember(data)) throw new Error("권한 변경 응답 형식이 올바르지 않습니다.");
    return data;
  },
};
