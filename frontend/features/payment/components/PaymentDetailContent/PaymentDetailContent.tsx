"use client";

import { useGetPaymentQuery } from "../../api/paymentApi";
import type { PaymentStatus } from "../../types/payment";
import styles from "./PaymentDetailContent.module.css";

interface PaymentDetailContentProps {
  orderId: string;
}

const STATUS_LABEL: Record<PaymentStatus, string> = {
  REQUESTED: "요청됨",
  COMPLETED: "결제완료",
  FAILED: "결제실패",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDate(value: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function PaymentDetailContent({ orderId }: PaymentDetailContentProps) {
  const { data: payment, isLoading, isError } = useGetPaymentQuery(orderId);

  if (isLoading) {
    return (
      <div className={styles.panel}>
        <p className={styles.state} aria-busy="true">불러오는 중...</p>
      </div>
    );
  }

  if (isError || !payment) {
    return (
      <div className={styles.panel}>
        <p className={styles.state}>결제 정보를 불러오지 못했습니다.</p>
      </div>
    );
  }

  const rows: { label: string; value: string }[] = [
    { label: "주문번호", value: payment.orderId },
    { label: "결제 금액", value: formatAmount(payment.amount) },
    { label: "결제 수단", value: payment.method },
    { label: "PG사", value: payment.pgProvider },
    { label: "거래 ID", value: payment.pgTransactionId ?? "-" },
    { label: "결제 일시", value: formatDate(payment.paidAt) },
    { label: "등록 일시", value: formatDate(payment.createdAt) },
  ];

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <h2 className={styles.title}>결제 정보</h2>
        <span className={styles.badge} data-status={payment.status}>
          {STATUS_LABEL[payment.status]}
        </span>
      </div>

      <div className={styles.rows}>
        {rows.map((row) => (
          <div key={row.label} className={styles.row}>
            <span className={styles.label}>{row.label}</span>
            <span className={styles.value}>{row.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
