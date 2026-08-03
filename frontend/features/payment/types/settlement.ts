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
