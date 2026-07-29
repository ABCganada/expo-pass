"use client";

import { Receipt } from "lucide-react";
import { useGetMyPaymentsQuery } from "../../api/paymentApi";
import { PaymentListItem } from "../PaymentListItem";
import styles from "./PaymentHistoryContent.module.css";

export function PaymentHistoryContent() {
  const { data: payments = [], isLoading } = useGetMyPaymentsQuery();

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>내 결제 내역</h1>
        <p className={styles.subtitle}>지금까지 결제한 티켓 내역을 확인할 수 있습니다.</p>
      </header>

      {isLoading && (
        <div className={styles.list}>
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && payments.length === 0 && (
        <div className={styles.emptyState}>
          <div className={styles.emptyIconWrap}>
            <Receipt size={28} />
          </div>
          <div className={styles.emptyBody}>
            <p className={styles.emptyTitle}>아직 결제 내역이 없습니다</p>
            <p className={styles.emptyDescription}>
              박람회 티켓을 예약하고 결제하면 여기서 내역을 확인할 수 있어요.
            </p>
          </div>
        </div>
      )}

      {!isLoading && payments.length > 0 && (
        <div className={styles.list}>
          {payments.map((payment) => (
            <PaymentListItem key={payment.id} payment={payment} />
          ))}
        </div>
      )}
    </div>
  );
}
