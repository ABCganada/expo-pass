"use client";

import { useEffect, useRef, useState } from "react";
import { MoreVertical } from "lucide-react";
import { AlertDialog } from "@/features/event/components/AlertDialog/AlertDialog";
import { ConfirmDialog } from "@/features/event/components/ConfirmDialog/ConfirmDialog";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { useGetPaymentQuery, useRequestRefundMutation } from "../../api/paymentApi";
import type { PaymentStatus } from "../../types/payment";
import styles from "./PaymentDetailContent.module.css";

interface PaymentDetailContentProps {
  orderId: string;
  hideRefund?: boolean;
  /** 예약 결제일 때만 넘어온다 — 행사 시작일 당일/이후엔 환불(취소)을 막는 데 쓴다. */
  eventStartDate?: string;
}

function isOnOrAfterStartDate(startDate: string): boolean {
  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  return todayStr >= startDate;
}

const STATUS_LABEL: Record<PaymentStatus, string> = {
  REQUESTED: "요청됨",
  COMPLETED: "결제완료",
  FAILED: "결제실패",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDate(value: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function PaymentDetailContent({ orderId, hideRefund = false, eventStartDate }: PaymentDetailContentProps) {
  const { data: payment, isLoading, isError } = useGetPaymentQuery(orderId);
  const [requestRefund, { isLoading: isRefunding }] = useRequestRefundMutation();
  const [refundMessage, setRefundMessage] = useState<string | null>(null);
  const [refundError, setRefundError] = useState<string | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [showRefundConfirm, setShowRefundConfirm] = useState(false);
  const [showRefundBlockedAlert, setShowRefundBlockedAlert] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  const handleRefund = async () => {
    if (!payment) return;

    setRefundMessage(null);
    setRefundError(null);

    try {
      await requestRefund({ paymentId: payment.id, reason: "사용자 요청" }).unwrap();
      setRefundMessage("환불 신청이 완료되었습니다.");
    } catch (error) {
      setRefundError(queryErrorMessage(error, "환불 신청에 실패했습니다."));
    }
  };

  if (isLoading) {
    return (
      <div className={styles.panel}>
        <p className={styles.state} aria-busy="true">불러오는 중...</p>
      </div>
    );
  }

  if (isError || !payment) {
    return (
      <div className={styles.panel}>
        <p className={styles.state}>결제 정보를 불러오지 못했습니다.</p>
      </div>
    );
  }

  const rows: { label: string; value: string }[] = [
    { label: "주문번호", value: payment.orderId },
    { label: "결제 금액", value: formatAmount(payment.amount) },
    { label: "결제 수단", value: payment.method },
    { label: "PG사", value: payment.pgProvider },
    { label: "거래 ID", value: payment.pgTransactionId ?? "-" },
    { label: "결제 일시", value: formatDate(payment.paidAt) },
    { label: "등록 일시", value: formatDate(payment.createdAt) },
  ];

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <h2 className={styles.title}>결제 정보</h2>
        <div className={styles.headerRight}>
          <span className={styles.badge} data-status={payment.status}>
            {STATUS_LABEL[payment.status]}
          </span>
          {!hideRefund && payment.status === "COMPLETED" && (
            <div className={styles.menuWrapper} ref={menuRef}>
              <button
                type="button"
                className={styles.menuButton}
                aria-label="더보기"
                onClick={() => setMenuOpen((v) => !v)}
              >
                <MoreVertical size={18} />
              </button>
              {menuOpen && (
                <div className={styles.menu}>
                  <button
                    type="button"
                    className={styles.menuItem}
                    onClick={() => {
                      setMenuOpen(false);
                      if (eventStartDate && isOnOrAfterStartDate(eventStartDate)) {
                        setShowRefundBlockedAlert(true);
                        return;
                      }
                      setShowRefundConfirm(true);
                    }}
                    disabled={isRefunding}
                  >
                    {isRefunding ? "환불 신청 중..." : "환불 신청"}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className={styles.rows}>
        {rows.map((row) => (
          <div key={row.label} className={styles.row}>
            <span className={styles.label}>{row.label}</span>
            <span className={styles.value}>{row.value}</span>
          </div>
        ))}
      </div>


      {refundMessage && <p className={styles.successMessage}>{refundMessage}</p>}
      {refundError && <p className={styles.errorMessage}>{refundError}</p>}

      {showRefundConfirm && (
        <ConfirmDialog
          title="이 결제를 환불 신청할까요?"
          confirmLabel="환불 신청"
          danger
          onConfirm={() => {
            setShowRefundConfirm(false);
            void handleRefund();
          }}
          onCancel={() => setShowRefundConfirm(false)}
        />
      )}

      {showRefundBlockedAlert && (
        <AlertDialog
          title="환불 신청이 불가능합니다"
          description="행사 시작일 이후에는 환불 신청을 할 수 없습니다."
          onConfirm={() => setShowRefundBlockedAlert(false)}
        />
      )}
    </div>
  );
}
