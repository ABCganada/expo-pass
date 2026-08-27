"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
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
  const router = useRouter();
  const [isRequesting, setIsRequesting] = useState(false);
  const tossPaymentsRef = useRef<TossPaymentsSDK | null>(null);

  const handleClick = async () => {
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
      // 결제창을 닫거나(UserCancelError) 위젯 렌더링 자체가 실패하는 등, 토스 서버까지
      // 도달하기 전에 클라이언트에서 끝나버리는 실패는 토스가 failUrl로 리다이렉트해주지
      // 않는다 — 실제 리다이렉트와 동일한 쿼리파라미터로 직접 이동시켜, PENDING 주문을
      // usePaymentResult의 실패 처리 경로로 그대로 태운다.
      const isUserCancel =
        thrown instanceof Error && thrown.name === "UserCancelError";
      const message =
        thrown instanceof Error ? thrown.message : "결제 요청에 실패했습니다.";
      const reason = isUserCancel ? "사용자가 결제를 취소했습니다." : message;
      router.push(
        `${failUrl}?orderId=${encodeURIComponent(orderId)}&message=${encodeURIComponent(reason)}`,
      );
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
        {isRequesting ? "결제 진행 중..." : "결제하기"}
      </button>
    </div>
  );
}
