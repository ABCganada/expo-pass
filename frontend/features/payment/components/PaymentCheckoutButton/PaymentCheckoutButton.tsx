"use client";

import { useRef, useState } from "react";
import { ANONYMOUS, loadTossPayments } from "@tosspayments/tosspayments-sdk";
import type { TossPaymentsSDK } from "@tosspayments/tosspayments-sdk";
import styles from "./PaymentCheckoutButton.module.css";

interface PaymentCheckoutButtonProps {
  orderId: string;
  amount: number;
  orderName: string;
  /** 결제 성공 시 돌아올 경로. 오리진을 뺀 상대경로(예: "/reservations/payment/success"). 주문 도메인마다 라우트가 다르므로 호출 측에서 지정한다. */
  successUrl: string;
  /** 결제 실패 시 돌아올 경로. successUrl과 동일하게 상대경로. */
  failUrl: string;
  disabled?: boolean;
}

const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "";

export function PaymentCheckoutButton({
  orderId,
  amount,
  orderName,
  successUrl,
  failUrl,
  disabled,
}: PaymentCheckoutButtonProps) {
  const [isRequesting, setIsRequesting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const tossPaymentsRef = useRef<TossPaymentsSDK | null>(null);

  const handleClick = async () => {
    setError(null);
    setIsRequesting(true);

    try {
      if (!tossPaymentsRef.current) {
        tossPaymentsRef.current = await loadTossPayments(TOSS_CLIENT_KEY);
      }
      const payment = tossPaymentsRef.current.payment({
        customerKey: ANONYMOUS,
      });

      await payment.requestPayment({
        method: "CARD",
        amount: { currency: "KRW", value: amount },
        orderId,
        orderName,
        successUrl: `${window.location.origin}${successUrl}`,
        failUrl: `${window.location.origin}${failUrl}`,
      });
    } catch (thrown) {
      // 사용자가 결제창을 닫은 경우(UserCancelError)는 에러로 취급하지 않는다.
      if (thrown instanceof Error && thrown.name === "UserCancelError") {
        return;
      }
      const message =
        thrown instanceof Error ? thrown.message : "결제 요청에 실패했습니다.";
      setError(message);
    } finally {
      setIsRequesting(false);
    }
  };

  return (
    <div className={styles.wrap}>
      <button
        type="button"
        className={styles.button}
        onClick={handleClick}
        disabled={disabled || isRequesting}
      >
        {isRequesting ? "결제 준비 중..." : "결제하기"}
      </button>
      {error && <p className={styles.error}>{error}</p>}
    </div>
  );
}
