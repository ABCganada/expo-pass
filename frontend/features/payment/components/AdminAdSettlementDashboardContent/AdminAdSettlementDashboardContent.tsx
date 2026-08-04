"use client";

import { Banknote, Hash, Wallet } from "lucide-react";
import { useGetAdSettlementDashboardQuery } from "../../api/adminApi";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminAdSettlementDashboardContent.module.css";

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export function AdminAdSettlementDashboardContent() {
  const { data, isLoading, isError, error } =
    useGetAdSettlementDashboardQuery();

  if (isLoading) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  if (isError || !data) {
    return (
      <div className={styles.state}>
        {queryErrorMessage(error, "광고 정산 대시보드를 불러오지 못했습니다.")}
      </div>
    );
  }

  const cards = [
    {
      label: "총 광고 매출",
      value: formatAmount(data.totalAmount),
      icon: Banknote,
    },
    {
      label: "순매출",
      value: formatAmount(data.totalNetAmount),
      icon: Wallet,
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
