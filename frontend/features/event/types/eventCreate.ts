import type { AdminEventStatus } from "./adminEvent";

export interface CreateEventPayload {
  title: string;
  categoryId: string;
  managerId: string;
}

export interface CreatedEvent {
  id: string;
  title: string;
  categoryName: string;
  managerId: string;
  status: AdminEventStatus;
}