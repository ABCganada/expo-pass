export type OrderStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "REFUNDED";

export type WaitingTicketStatus = "WAITING" | "ADMITTED" | "USED" | "EXPIRED";

export interface OrderItem {
  orderItemId: string;
  ticketId: string;
  unitPrice: number;
  qrCodeHash: string;
  checkedInAt: string | null;
}

export interface TicketQuantity {
  ticketId: string;
  quantity: number;
}

export interface OrderSummary {
  orderId: string;
  eventId: string;
  status: OrderStatus;
  totalAmount: number;
  reservedAt: string;
  ticketQuantities: TicketQuantity[];
}

export interface OrderDetail {
  orderId: string;
  userId: string;
  eventId: string;
  status: OrderStatus;
  totalAmount: number;
  reservedAt: string;
  items: OrderItem[];
}

export interface WaitingTicketResult {
  ticketNo: string;
  status: WaitingTicketStatus;
}

export interface WaitingStatusResult {
  status: WaitingTicketStatus;
  position: number | null;
}

export interface CreateOrderItemInput {
  ticketId: string;
  unitPrice: number;
  quantity: number;
}

export interface QrTicket {
  orderId: string;
  eventId: string;
  orderItemId: string;
  ticketId: string;
  qrCodeHash: string;
  checkedInAt: string | null;
}
