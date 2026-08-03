"use client";

import { PaymentDetailContent } from "@/features/payment/components/PaymentDetailContent";
import styles from "./AdPaymentModal.module.css";

interface AdPaymentModalProps {
  orderId: string;
  adTitle: string;
  onClose: () => void;
}

export function AdPaymentModal({ orderId, adTitle, onClose }: AdPaymentModalProps) {
  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>결제 정보</h2>
            <p className={styles.adTitle}>{adTitle}</p>
          </div>
          <button type="button" className={styles.btnClose} onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>
        <PaymentDetailContent orderId={orderId} hideRefund />
      </div>
    </div>
  );
}
