export type PaymentStatus =
  | "REQUESTED"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";
export type RefundStatus = "COMPLETED";
export type OrderType = "RESERVATION" | "ADVERTISEMENT";

export interface Payment {
  id: string;
  orderId: string;
  idempotencyKey: string;
  amount: number;
  method: string;
  status: PaymentStatus;
  pgProvider: string;
  pgOrderId: string | null;
  pgTransactionId: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  reason: string;
  status: RefundStatus;
  autoApproved: boolean;
  requestedAt: string;
  createdAt: string;
}

export interface PaymentConfirmRequest {
  orderType: OrderType;
  pgOrderId: string;
  paymentKey: string;
  amount: number;
}

export interface PaymentFailRequest {
  orderType: OrderType;
  reason: string;
}
