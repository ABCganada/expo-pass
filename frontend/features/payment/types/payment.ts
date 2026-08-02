export type PaymentStatus = "REQUESTED" | "COMPLETED" | "FAILED" | "CANCELLED" | "REFUNDED";

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
