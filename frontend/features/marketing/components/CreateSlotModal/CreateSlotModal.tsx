"use client";

import { useState } from "react";
import { useCreateSlotMutation, useUpdateSlotMutation } from "../../api/adminBannerApi";
import type { BannerSlot, BannerSlotType } from "../../types/marketerBanner";
import styles from "./CreateSlotModal.module.css";

interface CreateSlotModalProps {
  editTarget?: BannerSlot;
  onClose: () => void;
}

export function CreateSlotModal({ editTarget, onClose }: CreateSlotModalProps) {
  const isEdit = !!editTarget;
  const [name, setName] = useState(editTarget?.name ?? "");
  const [maxCount, setMaxCount] = useState(editTarget?.maxCount ?? 3);
  const [type, setType] = useState<BannerSlotType>(editTarget?.type ?? "BANNER");
  const [error, setError] = useState("");
  const [createSlot, { isLoading: creating }] = useCreateSlotMutation();
  const [updateSlot, { isLoading: updating }] = useUpdateSlotMutation();
  const isLoading = creating || updating;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      if (isEdit && editTarget) {
        await updateSlot({ id: editTarget.id, name, maxCount, type }).unwrap();
      } else {
        await createSlot({ name, maxCount, type }).unwrap();
      }
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "슬롯 저장에 실패했습니다.");
    }
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <h2 className={styles.title}>{isEdit ? "슬롯 수정" : "슬롯 생성"}</h2>
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
            <label className={styles.label}>슬롯 타입</label>
            <select
              className={styles.input}
              value={type}
              onChange={(e) => setType(e.target.value as BannerSlotType)}
              required
            >
              <option value="BANNER">배너형 (이미지 배너)</option>
              <option value="TAB">광고탭형 (광고 이미지)</option>
            </select>
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
          {error && <p className={styles.error}>{error}</p>}
          <div className={styles.actions}>
            <button type="button" className={styles.btnCancel} onClick={onClose}>취소</button>
            <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
              {isLoading ? "저장 중..." : isEdit ? "수정" : "생성"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
