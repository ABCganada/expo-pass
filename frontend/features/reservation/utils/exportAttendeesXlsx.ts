import * as XLSX from "xlsx";
import type { Attendee } from "../types/admin";
import type { OrderStatus } from "../types/reservation";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  CONFIRMED: "예약확정",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

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

/** 브라우저에서 곧바로 XLSX를 만들어 내려받는다 — 백엔드 호출 없이 이미 불러온 명단만 사용한다. */
export function exportAttendeesXlsx(attendees: Attendee[], eventId: string): void {
  const rows = attendees.map((attendee) => ({
    예약번호: attendee.orderId,
    예약자: attendee.userName ?? "탈퇴 사용자",
    이메일: attendee.userEmail ?? "",
    상태: STATUS_LABEL[attendee.status],
    금액: attendee.totalAmount,
    예약일시: formatDate(attendee.reservedAt),
  }));

  const sheet = XLSX.utils.json_to_sheet(rows);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, sheet, "예약자 명단");
  XLSX.writeFile(workbook, `attendees-${eventId}.xlsx`);
}
