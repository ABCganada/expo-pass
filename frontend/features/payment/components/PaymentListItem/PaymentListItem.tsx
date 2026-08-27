import type { Payment, PaymentStatus } from "../../types/payment";
import styles from "./PaymentListItem.module.css";

interface PaymentListItemProps {
  payment: Payment;
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

export function PaymentListItem({ payment }: PaymentListItemProps) {
  return (
    <article className={styles.row}>
      <div className={styles.main}>
        <p className={styles.orderId}>{payment.orderId}</p>
        <p className={styles.meta}>
          {payment.method} · {formatDate(payment.paidAt ?? payment.createdAt)}
        </p>
      </div>
      <div className={styles.trailing}>
        <span className={styles.amount}>{formatAmount(payment.amount)}</span>
        <span className={styles.badge} data-status={payment.status}>
          {STATUS_LABEL[payment.status]}
        </span>
      </div>
    </article>
  );
}
