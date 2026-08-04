import type { AdminEventStatus } from "./eventList";

export interface CreateEventPayload {
  title: string;
  categoryId: string;
}

export interface EventSummary {
  id: string;
  title: string;
  categoryName: string;
  managerId: string;
  status: AdminEventStatus;
}