"use client";

import { useRef, useState, useEffect } from "react";
import { QrCode, CheckCircle2, AlertCircle } from "lucide-react";
import { useCheckinMutation } from "../../api/adminApi";
import styles from "./CheckinContent.module.css";

export function CheckinContent() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [checkinResult, setCheckinResult] = useState<{
    type: "success" | "error";
    message: string;
    orderId?: string;
  } | null>(null);

  const [checkin, { isLoading }] = useCheckinMutation();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  async function handleCheckin() {
    const qrCode = inputRef.current?.value.trim();
    if (!qrCode) {
      setCheckinResult({ type: "error", message: "QR 코드를 입력해주세요" });
      return;
    }

    setCheckinResult(null);
    try {
      const result = await checkin(qrCode).unwrap();
      setCheckinResult({
        type: "success",
        message: `체크인 완료: ${result.orderId}`,
        orderId: result.orderId,
      });
      if (inputRef.current) inputRef.current.value = "";
      setTimeout(() => inputRef.current?.focus(), 100);
    } catch (e) {
      setCheckinResult({
        type: "error",
        message: e instanceof Error ? e.message : "체크인에 실패했습니다",
      });
    }
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>QR 체크인</h1>
        <p className={styles.subtitle}>QR 코드를 스캔하거나 입력하세요</p>
      </header>

      <div className={styles.card}>
        <div className={styles.qrIcon}>
          <QrCode size={48} />
        </div>

        <div className={styles.inputGroup}>
          <input
            ref={inputRef}
            type="text"
            autoFocus
            className={styles.input}
            placeholder="QR 코드 입력..."
            disabled={isLoading}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleCheckin();
            }}
          />
          <button
            type="button"
            className={styles.submitButton}
            onClick={handleCheckin}
            disabled={isLoading}
          >
            {isLoading ? "처리 중..." : "체크인"}
          </button>
        </div>

        {checkinResult && (
          <div className={styles.result} data-type={checkinResult.type}>
            {checkinResult.type === "success" ? (
              <CheckCircle2 size={20} />
            ) : (
              <AlertCircle size={20} />
            )}
            <span>{checkinResult.message}</span>
          </div>
        )}
      </div>

      <div className={styles.tips}>
        <h3>사용 방법</h3>
        <ul>
          <li>QR 코드 스캔기를 사용하거나 입력창에 직접 붙여넣으세요</li>
          <li>Enter 키를 눌러 체크인을 진행합니다</li>
          <li>이미 체크인된 티켓은 오류가 발생합니다</li>
          <li>존재하지 않는 QR 코드는 오류가 발생합니다</li>
        </ul>
      </div>
    </div>
  );
}
