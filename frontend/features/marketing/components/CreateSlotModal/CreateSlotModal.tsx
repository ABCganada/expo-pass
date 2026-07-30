"use client";

import { useState } from "react";
import { useCreateSlotMutation } from "../../api/adminBannerApi";
import styles from "./CreateSlotModal.module.css";

interface CreateSlotModalProps {
  onClose: () => void;
}

export function CreateSlotModal({ onClose }: CreateSlotModalProps) {
  const [name, setName] = useState("");
  const [maxCount, setMaxCount] = useState(3);
  const [pricePerDay, setPricePerDay] = useState(30000);
  const [error, setError] = useState("");
  const [createSlot, { isLoading }] = useCreateSlotMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      await createSlot({ name, maxCount, pricePerDay }).unwrap();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "슬롯 생성에 실패했습니다.");
    }
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <h2 className={styles.title}>슬롯 생성</h2>
        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label}>슬롯 이름</label>
            <input
              className={styles.input}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 메인 상단 배너"
              required
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>최대 광고 수</label>
            <input
              type="number"
              className={styles.input}
              value={maxCount}
              onChange={(e) => setMaxCount(Number(e.target.value))}
              min={1}
              max={20}
              required
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>일 단가 (원)</label>
            <input
              type="number"
              className={styles.input}
              value={pricePerDay}
              onChange={(e) => setPricePerDay(Number(e.target.value))}
              min={0}
              step={1000}
              required
            />
          </div>
          {error && <p className={styles.error}>{error}</p>}
          <div className={styles.actions}>
            <button type="button" className={styles.btnCancel} onClick={onClose}>취소</button>
            <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
              {isLoading ? "생성 중..." : "생성"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
