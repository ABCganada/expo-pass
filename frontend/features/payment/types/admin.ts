export interface DashboardSummary {
  totalSales: number;
  totalCommissionAmount: number;
  settlementCount: number;
}

export interface AdSettlementDashboardSummary {
  totalAmount: number;
  totalNetAmount: number;
  settlementCount: number;
}

export interface PaymentLog {
  id: string;
  paymentKey: string | null;
  action: string;
  requestPayload: string | null;
  responsePayload: string | null;
  webhookTransmissionId: string | null;
  createdAt: string;
}

export type PaymentLogDetail = PaymentLog;

export interface PaymentLogPage {
  logs: PaymentLog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
