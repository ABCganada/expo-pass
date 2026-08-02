import { getCsrfToken } from "@/features/shared/api/csrf";
import type { EventCategory } from "../types/event";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
const BASE = `${API_BASE_URL}/api/v1/admin/events/categories`;

async function parseData<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok || !body.success) throw new Error(body.message ?? "요청에 실패했습니다.");
  return body.data as T;
}

export const adminCategoryService = {
  getAllCategories: (signal?: AbortSignal): Promise<EventCategory[]> =>
    fetch(BASE, { credentials: "include", signal }).then((res) => parseData<EventCategory[]>(res)),

  toggleActive: async (categoryId: string): Promise<EventCategory> => {
    const csrf = await getCsrfToken();
    const res = await fetch(`${BASE}/${categoryId}/active`, {
      method: "PATCH",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    });
    return parseData<EventCategory>(res);
  },
};