"use client";

import { Download, X } from "lucide-react";
import {
  useGetSettlementDetailQuery,
  useGetSettlementPaymentsQuery,
} from "../../api/settlementApi";
import { exportSettlementXlsx } from "../../utils/exportSettlementXlsx";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./ManagerSettlementDetailModal.module.css";

interface ManagerSettlementDetailModalProps {
  settlementId: string;
  onClose: () => void;
}

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

export function ManagerSettlementDetailModal({
  settlementId,
  onClose,
}: ManagerSettlementDetailModalProps) {
  const {
    data: settlement,
    isLoading,
    isError,
    error,
  } = useGetSettlementDetailQuery(settlementId);
  const {
    data: payments,
    isLoading: isPaymentsLoading,
    isError: isPaymentsError,
    error: paymentsError,
  } = useGetSettlementPaymentsQuery(settlementId);

  const rows: { label: string; value: React.ReactNode; emphasis?: boolean }[] =
    settlement
      ? [
          { label: "행사 ID", value: settlement.eventId },
          { label: "총 매출", value: formatAmount(settlement.totalSales) },
          {
            label: `수수료 (${settlement.commissionRate.toFixed(2)}%)`,
            value: formatAmount(settlement.commissionAmount),
          },
          {
            label: "순매출",
            value: formatAmount(settlement.netAmount),
            emphasis: true,
          },
          { label: "정산일", value: formatDate(settlement.settledAt) },
        ]
      : [];

  return (
    <div
      className={styles.overlay}
      onClick={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className={styles.modal}>
        <div className={styles.header}>
          <h2 className={styles.title}>정산 상세</h2>
          <button
            type="button"
            className={styles.closeButton}
            onClick={onClose}
            aria-label="닫기"
          >
            <X className={styles.closeIcon} aria-hidden="true" />
          </button>
        </div>

        {isLoading ? (
          <p className={styles.state}>불러오는 중...</p>
        ) : isError || !settlement ? (
          <p className={styles.state}>
            {queryErrorMessage(error, "정산 상세 정보를 불러오지 못했습니다.")}
          </p>
        ) : (
          <>
            <div className={styles.rows}>
              {rows.map((row) => (
                <div key={row.label} className={styles.row}>
                  <span className={styles.label}>{row.label}</span>
                  <span
                    className={
                      row.emphasis ? styles.valueEmphasis : styles.value
                    }
                  >
                    {row.value}
                  </span>
                </div>
              ))}
            </div>

            <div className={styles.paymentsSection}>
              <h3 className={styles.paymentsTitle}>
                포함된 결제 내역{payments ? ` (${payments.length}건)` : ""}
              </h3>

              {isPaymentsLoading ? (
                <p className={styles.state}>불러오는 중...</p>
              ) : isPaymentsError ? (
                <p className={styles.state}>
                  {queryErrorMessage(
                    paymentsError,
                    "결제 내역을 불러오지 못했습니다.",
                  )}
                </p>
              ) : !payments || payments.length === 0 ? (
                <p className={styles.state}>포함된 결제 내역이 없습니다.</p>
              ) : (
                <div className={styles.paymentsTableWrap}>
                  <table className={styles.paymentsTable}>
                    <thead>
                      <tr>
                        <th>주문번호</th>
                        <th>결제수단</th>
                        <th>금액</th>
                        <th>환불액</th>
                        <th>최종 결제 금액</th>
                        <th>결제일시</th>
                      </tr>
                    </thead>
                    <tbody>
                      {payments.map((payment) => (
                        <tr key={payment.id}>
                          <td className={styles.mono}>{payment.orderId}</td>
                          <td>{payment.method}</td>
                          <td>{formatAmount(payment.amount)}</td>
                          <td>
                            {payment.refundAmount > 0
                              ? `-${formatAmount(payment.refundAmount)}`
                              : "-"}
                          </td>
                          <td>
                            {formatAmount(
                              payment.amount - payment.refundAmount,
                            )}
                          </td>
                          <td>
                            {formatDate(payment.paidAt ?? payment.createdAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className={styles.actions}>
              <button
                type="button"
                className={styles.downloadButton}
                onClick={() => exportSettlementXlsx(settlement)}
              >
                <Download size={16} aria-hidden="true" />
                리포트 다운로드
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
