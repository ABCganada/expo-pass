"use client";

import { useState } from "react";
import type { BannerSlot, MarketerBannerAd } from "../../types/marketerBanner";
import { useGetBannerSlotsQuery, useRegisterAdMutation, useUpdateAdMutation } from "../../api/marketerBannerApi";
import styles from "./RegisterAdModal.module.css";

interface RegisterAdModalProps {
  editTarget?: MarketerBannerAd;
  onClose: () => void;
}

function toDatetimeLocal(iso: string) {
  return iso ? iso.slice(0, 16) : "";
}

function toOffsetDateTime(local: string) {
  if (!local) return "";
  // datetime-local 값은 브라우저 로컬 시간 기준 "YYYY-MM-DDTHH:mm"
  // 로컬 타임존 오프셋을 직접 붙여야 올바른 OffsetDateTime이 된다.
  // (toISOString()은 UTC 변환 후 Z를 +09:00으로 바꾸므로 9시간 앞당겨지는 버그 발생)
  const date = new Date(local);
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const absOffset = Math.abs(offsetMinutes);
  const hh = String(Math.floor(absOffset / 60)).padStart(2, "0");
  const mm = String(absOffset % 60).padStart(2, "0");
  return `${local}:00${sign}${hh}:${mm}`;
}

export function RegisterAdModal({ editTarget, onClose }: RegisterAdModalProps) {
  const isEdit = !!editTarget;
  const { data: slots = [] } = useGetBannerSlotsQuery();
  const [registerAd, { isLoading: registering }] = useRegisterAdMutation();
  const [updateAd, { isLoading: updating }] = useUpdateAdMutation();

  const [form, setForm] = useState({
    slotId: editTarget?.slotId ?? "",
    title: editTarget?.title ?? "",
    imageUrl: editTarget?.imageUrl ?? "",
    linkUrl: editTarget?.linkUrl ?? "",
    priority: editTarget?.priority ?? 1,
    startsAt: editTarget ? toDatetimeLocal(editTarget.startsAt) : "",
    endsAt: editTarget ? toDatetimeLocal(editTarget.endsAt) : "",
  });
  const [error, setError] = useState("");
  const isLoading = registering || updating;

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const payload = {
        ...form,
        priority: Number(form.priority),
        startsAt: toOffsetDateTime(form.startsAt),
        endsAt: toOffsetDateTime(form.endsAt),
      };
      if (isEdit && editTarget) {
        const { slotId: _, ...updatePayload } = payload;
        await updateAd({ id: editTarget.id, ...updatePayload }).unwrap();
      } else {
        await registerAd(payload).unwrap();
      }
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    }
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <h2 className={styles.title}>{isEdit ? "광고 수정" : "광고 등록"}</h2>
        <form className={styles.form} onSubmit={handleSubmit}>
          {!isEdit && (
            <div className={styles.field}>
              <label className={styles.label}>광고 슬롯</label>
              <select className={styles.select} value={form.slotId} onChange={set("slotId")} required>
                <option value="">슬롯 선택</option>
                {slots.map((slot: BannerSlot) => (
                  <option key={slot.id} value={slot.id}>{slot.name}</option>
                ))}
              </select>
            </div>
          )}
          <div className={styles.field}>
            <label className={styles.label}>광고 제목</label>
            <input className={styles.input} value={form.title} onChange={set("title")} placeholder="광고 제목" required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>이미지 URL</label>
            <input className={styles.input} value={form.imageUrl} onChange={set("imageUrl")} placeholder="https://..." required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>랜딩 URL</label>
            <input className={styles.input} value={form.linkUrl} onChange={set("linkUrl")} placeholder="https://..." required />
          </div>
          <div className={styles.row}>
            <div className={styles.field}>
              <label className={styles.label}>시작일시</label>
              <input type="datetime-local" className={styles.input} value={form.startsAt} onChange={set("startsAt")} required />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>종료일시</label>
              <input type="datetime-local" className={styles.input} value={form.endsAt} onChange={set("endsAt")} required />
            </div>
          </div>
          <div className={styles.field}>
            <label className={styles.label}>우선순위</label>
            <input type="number" className={styles.input} value={form.priority} onChange={set("priority")} min={1} max={100} required />
          </div>
          {error && <p className={styles.error}>{error}</p>}
          <div className={styles.actions}>
            <button type="button" className={styles.btnCancel} onClick={onClose}>취소</button>
            <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
              {isLoading ? "저장 중..." : isEdit ? "수정" : "등록"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
