"use client";

import { useState } from "react";
import { useGetPaymentLogsQuery } from "../../api/adminApi";
import { actionTone } from "../../utils/actionTone";
import { AdminPaymentLogDetailContent } from "../AdminPaymentLogDetailContent";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminPaymentLogListContent.module.css";

const PAGE_SIZE = 20;

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function AdminPaymentLogListContent() {
  const [page, setPage] = useState(0);
  const [selectedLogId, setSelectedLogId] = useState<string | undefined>(
    undefined,
  );
  const { data, isLoading, isFetching, isError, error } =
    useGetPaymentLogsQuery({ page, size: PAGE_SIZE });

  const logs = data?.logs ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className={styles.panel}>
      {isError ? (
        <div className={styles.state}>
          {queryErrorMessage(error, "결제 로그를 불러오지 못했습니다.")}
        </div>
      ) : isLoading ? (
        <div className={styles.state}>불러오는 중...</div>
      ) : logs.length === 0 ? (
        <div className={styles.state}>표시할 결제 로그가 없습니다.</div>
      ) : (
        <div className={styles.tableWrap} data-fetching={isFetching}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>결제 Key</th>
                <th>액션</th>
                <th>웹훅 전송 ID</th>
                <th>발생 일시</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr
                  key={log.id}
                  className={styles.row}
                  role="button"
                  tabIndex={0}
                  aria-label={`결제 로그 ${log.id} 상세 보기`}
                  onClick={() => setSelectedLogId(log.id)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedLogId(log.id);
                    }
                  }}
                >
                  <td className={styles.mono}>{log.paymentKey ?? "-"}</td>
                  <td>
                    <span
                      className={styles.actionBadge}
                      data-tone={actionTone(log.action)}
                    >
                      {log.action}
                    </span>
                  </td>
                  <td className={styles.mono}>
                    {log.webhookTransmissionId ?? "-"}
                  </td>
                  <td>{formatDate(log.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 ? (
        <footer className={styles.pagination}>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page <= 0 || isFetching}
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
          >
            이전
          </button>
          <span className={styles.pageInfo}>
            {page + 1} / {totalPages} (총 {totalElements}건)
          </span>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page + 1 >= totalPages || isFetching}
            onClick={() => setPage((value) => value + 1)}
          >
            다음
          </button>
        </footer>
      ) : null}

      {selectedLogId && (
        <AdminPaymentLogDetailContent
          id={selectedLogId}
          onClose={() => setSelectedLogId(undefined)}
        />
      )}
    </div>
  );
}
