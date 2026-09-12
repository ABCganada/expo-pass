"use client";

import { Banknote, Hash, Percent } from "lucide-react";
import { useGetPaymentDashboardQuery } from "../../api/adminApi";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminPaymentDashboardContent.module.css";

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export function AdminPaymentDashboardContent() {
  const { data, isLoading, isError, error } = useGetPaymentDashboardQuery();

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  if (isError || !data) {
    return (
      <div className={styles.state}>
        {queryErrorMessage(error, "매출 대시보드를 불러오지 못했습니다.")}
      </div>
    );
  }

  const cards = [
    {
      label: "박람회 총 매출",
      value: formatAmount(data.totalSales),
      icon: Banknote,
    },
    {
      // 플랫폼이 실제로 버는 돈은 수수료다 — 나머지는 행사 주최자에게 지급되는
      // 금액이라 플랫폼 매출이 아니므로 대시보드에는 올리지 않는다.
      label: "총 수수료",
      value: formatAmount(data.totalCommissionAmount),
      icon: Percent,
      emphasis: true,
    },
    {
      label: "정산 건수",
      value: `${data.settlementCount.toLocaleString("ko-KR")}건`,
      icon: Hash,
    },
  ];

  return (
    <div className={styles.container}>
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <div
            key={card.label}
            className={styles.segment}
            data-emphasis={card.emphasis ?? false}
          >
            <div className={styles.iconBadge}>
              <Icon className={styles.icon} aria-hidden="true" />
            </div>
            <div className={styles.text}>
              <p className={styles.label}>{card.label}</p>
              <p className={styles.value}>{card.value}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
}
