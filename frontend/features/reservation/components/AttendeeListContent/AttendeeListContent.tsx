"use client";

import { useState } from "react";
import { Download, Mail, User } from "lucide-react";
import { useGetEventAttendeesQuery } from "../../api/adminApi";
import { exportAttendeesXlsx } from "../../utils/exportAttendeesXlsx";
import type { OrderStatus } from "../../types/reservation";
import styles from "./AttendeeListContent.module.css";

interface AttendeeListContentProps {
  eventId: string;
}

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  CONFIRMED: "예약확정",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

function formatAmount(value: number): string {
  return `${value.toLocaleString("ko-KR")}원`;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

export function AttendeeListContent({ eventId }: AttendeeListContentProps) {
  const { data: attendees = [], isLoading } = useGetEventAttendeesQuery(eventId);
  const [downloadError, setDownloadError] = useState("");

  const handleDownload = () => {
    setDownloadError("");
    try {
      exportAttendeesXlsx(attendees, eventId);
    } catch {
      setDownloadError("다운로드에 실패했습니다.");
    }
  };

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.subtitle}>행사 예약자 {attendees.length}명</p>
          {downloadError && <p className={styles.downloadError}>{downloadError}</p>}
        </div>
        <button
          type="button"
          className={styles.downloadButton}
          onClick={handleDownload}
          disabled={attendees.length === 0}
        >
          <Download size={16} />
          XLSX 다운로드
        </button>
      </header>

      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>예약번호</th>
              <th>예약자</th>
              <th>이메일</th>
              <th>상태</th>
              <th>금액</th>
              <th>예약일시</th>
            </tr>
          </thead>
          <tbody>
            {attendees.map((attendee) => (
              <tr key={attendee.orderId}>
                <td className={styles.orderId}>{attendee.orderId}</td>
                <td className={styles.name}>
                  <span className={styles.iconCell}>
                    <User size={14} />
                    {attendee.userName ?? "탈퇴 사용자"}
                  </span>
                </td>
                <td className={styles.email}>
                  <span className={styles.iconCell}>
                    <Mail size={14} />
                    {attendee.userEmail ?? "—"}
                  </span>
                </td>
                <td className={styles.status} data-status={attendee.status}>
                  {STATUS_LABEL[attendee.status]}
                </td>
                <td className={styles.amount}>{formatAmount(attendee.totalAmount)}</td>
                <td className={styles.date}>{formatDate(attendee.reservedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
