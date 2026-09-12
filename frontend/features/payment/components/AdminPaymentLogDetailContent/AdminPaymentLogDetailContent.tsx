"use client";

import { X } from "lucide-react";
import { useGetPaymentLogDetailQuery } from "../../api/adminApi";
import { actionTone } from "../../utils/actionTone";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminPaymentLogDetailContent.module.css";

interface AdminPaymentLogDetailContentProps {
  id: string;
  onClose: () => void;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function formatPayload(payload: string | null): string | null {
  if (!payload) return null;
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

export function AdminPaymentLogDetailContent({
  id,
  onClose,
}: AdminPaymentLogDetailContentProps) {
  const {
    data: log,
    isLoading,
    isError,
    error,
  } = useGetPaymentLogDetailQuery(id);

  const requestPayload = log ? formatPayload(log.requestPayload) : null;
  const responsePayload = log ? formatPayload(log.responsePayload) : null;

  const rows: { label: string; value: React.ReactNode }[] = log
    ? [
        { label: "결제 Key", value: log.paymentKey ?? "-" },
        {
          label: "액션",
          value: (
            <span
              className={styles.actionBadge}
              data-tone={actionTone(log.action)}
            >
              {log.action}
            </span>
          ),
        },
        { label: "웹훅 전송 ID", value: log.webhookTransmissionId ?? "-" },
        { label: "발생 일시", value: formatDate(log.createdAt) },
      ]
    : [];

  return (
    <div
      className={styles.overlay}
      onClick={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className={styles.modal}>
        <div className={styles.header}>
          <h2 className={styles.title}>결제 로그 상세</h2>
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
        ) : isError || !log ? (
          <p className={styles.state}>
            {queryErrorMessage(error, "결제 로그를 불러오지 못했습니다.")}
          </p>
        ) : (
          <>
            <div className={styles.rows}>
              {rows.map((row) => (
                <div key={row.label} className={styles.row}>
                  <span className={styles.label}>{row.label}</span>
                  <span className={styles.value}>{row.value}</span>
                </div>
              ))}
            </div>

            <div className={styles.payloadSection}>
              <h3 className={styles.payloadTitle}>요청 페이로드</h3>
              {requestPayload ? (
                <pre className={styles.payload}>{requestPayload}</pre>
              ) : (
                <p className={styles.state}>없음</p>
              )}
            </div>

            <div className={styles.payloadSection}>
              <h3 className={styles.payloadTitle}>응답 페이로드</h3>
              {responsePayload ? (
                <pre className={styles.payload}>{responsePayload}</pre>
              ) : (
                <p className={styles.state}>없음</p>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
