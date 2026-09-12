export type AdminEventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";

export interface AdminEventListItem {
  id: string;
  title: string;
  categoryName: string;
  managerId: string;
  status: AdminEventStatus;
  startDate: string | null;
  endDate: string | null;
  phase: string | null;
  viewCount: number;
  thumbnailUrl: string | null;
  createdAt: string;
}