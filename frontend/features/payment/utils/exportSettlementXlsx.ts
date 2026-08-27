import * as XLSX from "xlsx";
import type { Settlement, SettlementPayment } from "../types/settlement";

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

/**
 * 브라우저에서 곧바로 XLSX를 만들어 내려받는다 — 백엔드 호출 없이 이미 불러온 정산/결제 데이터만 사용한다.
 * "정산 개요" 시트(요약) + "결제 내역" 시트(정산에 포함된 결제 목록) 두 장으로 구성된다.
 */
export function exportSettlementXlsx(
  settlement: Settlement,
  payments: SettlementPayment[],
): void {
  const summaryRows = [
    {
      "정산 ID": settlement.id,
      "행사 ID": settlement.eventId,
      "총 매출": settlement.totalSales,
      "수수료율 (%)": settlement.commissionRate,
      수수료: settlement.commissionAmount,
      순매출: settlement.netAmount,
      정산일: formatDate(settlement.settledAt),
    },
  ];

  const paymentRows = payments.map((payment) => ({
    주문번호: payment.orderId,
    결제수단: payment.method,
    금액: payment.amount,
    환불액: payment.refundAmount,
    "최종 결제 금액": payment.amount - payment.refundAmount,
    결제일시: formatDate(payment.paidAt ?? payment.createdAt),
  }));

  const summarySheet = XLSX.utils.json_to_sheet(summaryRows);
  const paymentsSheet = XLSX.utils.json_to_sheet(paymentRows);

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, summarySheet, "정산 개요");
  XLSX.utils.book_append_sheet(workbook, paymentsSheet, "결제 내역");
  XLSX.writeFile(workbook, `settlement-${settlement.id}.xlsx`);
}
