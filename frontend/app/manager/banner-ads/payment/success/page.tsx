"use client";

import { Suspense } from "react";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";
import { usePaymentResult } from "@/features/payment/hooks/usePaymentResult";
import Link from "next/link";
import styles from "./page.module.css";

function AdPaymentSuccessContent() {
  const { status, orderId, amount, failReason } = usePaymentResult("ADVERTISEMENT");

  if (status === "loading") {
    return (
      <div className={styles.panel}>
        <Loader2 size={40} className={styles.iconLoading} />
        <p className={styles.title}>결제 확인 중...</p>
      </div>
    );
  }

  if (status === "success") {
    return (
      <div className={styles.panel}>
        <CheckCircle2 size={40} className={styles.iconSuccess} />
        <p className={styles.title}>결제가 완료되었습니다</p>
        <p className={styles.desc}>광고 검토 후 승인되면 게재가 시작돼요.</p>
        <div className={styles.detail}>
          {orderId && (
            <div className={styles.row}>
              <span>주문번호</span>
              <strong>{orderId}</strong>
            </div>
          )}
          {amount != null && (
            <div className={styles.row}>
              <span>결제금액</span>
              <strong>{amount.toLocaleString("ko-KR")}원</strong>
            </div>
          )}
        </div>
        <Link href="/manager/banner-ads" className={styles.btn}>
          광고 관리로 돌아가기
        </Link>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <XCircle size={40} className={styles.iconFail} />
      <p className={styles.title}>결제에 실패했습니다</p>
      {failReason && <p className={styles.desc}>{failReason}</p>}
      <Link href="/manager/banner-ads" className={styles.btn}>
        광고 관리로 돌아가기
      </Link>
    </div>
  );
}

export default function AdPaymentSuccessPage() {
  return (
    <div className={styles.page}>
      <Suspense
        fallback={
          <div className={styles.panel}>
            <Loader2 size={40} className={styles.iconLoading} />
            <p className={styles.title}>결제 결과 확인 중...</p>
          </div>
        }
      >
        <AdPaymentSuccessContent />
      </Suspense>
    </div>
  );
}
