"use client";

import Link from "next/link";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { usePaymentResult } from "@/features/payment/hooks/usePaymentResult";
import { useGetOrderQuery } from "../../api/reservationApi";
import styles from "./PaymentResultContent.module.css";

export function PaymentResultContent() {
  const result = usePaymentResult("RESERVATION");
  const { data: order } = useGetOrderQuery(result.orderId ?? "", {
    skip: result.status !== "success" || !result.orderId,
  });

  if (result.status === "loading") {
    return (
      <div className={styles.page}>
        <div className={styles.card}>
          <Loader2 size={40} className={styles.spinner} />
          <p className={styles.title}>결제 확인 중...</p>
        </div>
      </div>
    );
  }

  if (result.status === "success") {
    return (
      <div className={styles.page}>
        <div className={styles.card}>
          <CheckCircle2 size={40} className={styles.successIcon} />
          <p className={styles.title}>결제가 완료되었습니다</p>
          <div className={styles.detail}>
            <div className={styles.detailRow}>
              <span>예약번호</span>
              <strong>{result.orderId}</strong>
            </div>
            {order && (
              <div className={styles.detailRow}>
                <span>티켓 수량</span>
                <strong>{order.items.length}매</strong>
              </div>
            )}
            <div className={styles.detailRow}>
              <span>결제금액</span>
              <strong>{(result.amount ?? 0).toLocaleString("ko-KR")}원</strong>
            </div>
          </div>
          <Link href="/reservations" className={styles.primaryButton}>
            예약 내역 바로가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <XCircle size={40} className={styles.failIcon} />
        <p className={styles.title}>{result.status === "invalid" ? "잘못된 접근입니다" : "결제에 실패했습니다"}</p>
        {result.failReason && <p className={styles.description}>{result.failReason}</p>}
        <Link href="/reservations" className={styles.primaryButton}>
          예약 내역으로 이동
        </Link>
      </div>
    </div>
  );
}
