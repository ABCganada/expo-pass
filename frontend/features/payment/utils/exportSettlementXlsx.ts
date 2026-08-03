import * as XLSX from "xlsx";
import type { Settlement } from "../types/settlement";

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

/** 브라우저에서 곧바로 XLSX를 만들어 내려받는다 — 백엔드 호출 없이 이미 불러온 정산 데이터만 사용한다. */
export function exportSettlementXlsx(settlement: Settlement): void {
  const rows = [
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

  const sheet = XLSX.utils.json_to_sheet(rows);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, sheet, "정산 리포트");
  XLSX.writeFile(workbook, `settlement-${settlement.id}.xlsx`);
}
