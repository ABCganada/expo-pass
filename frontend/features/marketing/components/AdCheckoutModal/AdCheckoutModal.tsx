"use client";

import { PaymentCheckoutButton } from "@/features/payment/components/PaymentCheckoutButton";
import styles from "./AdCheckoutModal.module.css";

interface AdCheckoutModalProps {
  orderId: string;
  amount: number;
  orderName: string;
  onClose: () => void;
}

export function AdCheckoutModal({ orderId, amount, orderName, onClose }: AdCheckoutModalProps) {
  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <h2 className={styles.title}>광고 결제</h2>
        <div className={styles.summary}>
          <div className={styles.row}>
            <span>광고 제목</span>
            <strong>{orderName}</strong>
          </div>
          <div className={styles.row}>
            <span>결제 금액</span>
            <strong>{amount.toLocaleString("ko-KR")}원</strong>
          </div>
        </div>
        <PaymentCheckoutButton
          orderId={orderId}
          amount={amount}
          orderName={orderName}
          successUrl="/manager/banner-ads/payment/success"
          failUrl="/manager/banner-ads/payment/fail"
        />
        <button type="button" className={styles.btnCancel} onClick={onClose}>
          닫기
        </button>
      </div>
    </div>
  );
}
