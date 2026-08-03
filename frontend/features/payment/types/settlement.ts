import type { PaymentStatus } from "./payment";

export type SettlementStatus = "COMPLETED";

export interface Settlement {
  id: string;
  eventId: string;
  totalSales: number;
  commissionRate: number;
  commissionAmount: number;
  netAmount: number;
  status: SettlementStatus;
  settledAt: string;
  createdAt: string;
}

// 정산 총매출 계산에 쓰인 완료 결제 목록(감사용). amount는 환불 차감 전 원 금액,
// refundAmount는 이 결제에 걸린 활성 환불액(없으면 0)이다.
export interface SettlementPayment {
  id: string;
  orderId: string;
  amount: number;
  refundAmount: number;
  method: string;
  status: PaymentStatus;
  pgProvider: string;
  pgTransactionId: string | null;
  paidAt: string | null;
  createdAt: string;
}
