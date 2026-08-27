import type { OrderStatus } from "./reservation";

export interface Attendee {
  orderId: string;
  userId: string;
  userName: string | null;
  userEmail: string | null;
  status: OrderStatus;
  totalAmount: number;
  reservedAt: string;
}

export interface CheckinRequest {
  qrCodeHash: string;
}

export interface CheckinResponse {
  orderItemId: string;
  orderId: string;
  ticketId: string;
  checkedInAt: string | null;
}

export interface EventReservationSummary {
  eventId: string;
  totalOrders: number;
  countsByStatus: Record<OrderStatus, number>;
}

export interface CheckinProgress {
  totalItems: number;
  checkedInCount: number;
}

export interface DailyReservationCount {
  date: string;
  count: number;
}
