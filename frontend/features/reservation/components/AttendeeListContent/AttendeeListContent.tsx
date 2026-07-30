"use client";

import { Mail, User } from "lucide-react";
import { useGetEventAttendeesQuery } from "../../api/adminApi";
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

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>예약자 명단</h1>
        <p className={styles.subtitle}>행사 예약자 {attendees.length}명</p>
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
                  <User size={14} />
                  {attendee.userName ?? "탈퇴 사용자"}
                </td>
                <td className={styles.email}>
                  <Mail size={14} />
                  {attendee.userEmail ?? "—"}
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
