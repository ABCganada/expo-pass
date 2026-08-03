"use client";

import { Suspense } from "react";
import { XCircle, Loader2 } from "lucide-react";
import { usePaymentResult } from "@/features/payment/hooks/usePaymentResult";
import Link from "next/link";
import styles from "./page.module.css";

function AdPaymentFailContent() {
  const { failReason } = usePaymentResult("ADVERTISEMENT");

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

export default function AdPaymentFailPage() {
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
        <AdPaymentFailContent />
      </Suspense>
    </div>
  );
}
