"use client";

import { useState } from "react";
import { CircleHelp } from "lucide-react";
import { useGetSettlementsQuery } from "../../api/settlementApi";
import { ManagerSettlementDetailModal } from "../ManagerSettlementDetailModal";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./ManagerSettlementListContent.module.css";

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function ManagerSettlementListContent() {
  const { data, isLoading, isFetching, isError, error } =
    useGetSettlementsQuery();
  const [selectedSettlementId, setSelectedSettlementId] = useState<
    string | undefined
  >(undefined);

  const settlements = data ?? [];

  return (
    <div className={styles.panel}>
      {isError ? (
        <div className={styles.state}>
          {queryErrorMessage(error, "정산 목록을 불러오지 못했습니다.")}
        </div>
      ) : isLoading ? (
        <div className={styles.state}>불러오는 중...</div>
      ) : settlements.length === 0 ? (
        <div className={styles.state}>표시할 정산 내역이 없습니다.</div>
      ) : (
        <div className={styles.tableWrap} data-fetching={isFetching}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>행사 ID</th>
                <th>총 매출</th>
                <th>
                  <span className={styles.thWithHelp}>
                    수수료
                    <span
                      className={styles.helpTrigger}
                      tabIndex={0}
                      onClick={(event) => event.stopPropagation()}
                    >
                      <CircleHelp
                        aria-hidden="true"
                        className={styles.helpIcon}
                      />
                      <span className={styles.tooltip} role="tooltip">
                        플랫폼 수수료율은 5%로 고정되어 있습니다.
                      </span>
                    </span>
                  </span>
                </th>
                <th>순매출</th>
                <th>정산일</th>
              </tr>
            </thead>
            <tbody>
              {settlements.map((settlement) => (
                <tr
                  key={settlement.id}
                  className={styles.row}
                  role="button"
                  tabIndex={0}
                  aria-label={`정산 ${settlement.id} 상세 보기`}
                  onClick={() => setSelectedSettlementId(settlement.id)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedSettlementId(settlement.id);
                    }
                  }}
                >
                  <td className={styles.mono}>{settlement.eventId}</td>
                  <td>{formatAmount(settlement.totalSales)}</td>
                  <td>{formatAmount(settlement.commissionAmount)}</td>
                  <td className={styles.netAmount}>
                    {formatAmount(settlement.netAmount)}
                  </td>
                  <td>{formatDate(settlement.settledAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedSettlementId && (
        <ManagerSettlementDetailModal
          settlementId={selectedSettlementId}
          onClose={() => setSelectedSettlementId(undefined)}
        />
      )}
    </div>
  );
}
