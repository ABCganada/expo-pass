"use client";

import { useRef, useState } from "react";
import type { BannerSlot, MarketerBannerAd } from "../../types/marketerBanner";
import { BANNER_SLOT_TYPE_LABEL } from "../../types/marketerBanner";
import { useGetBannerSlotsQuery, useRegisterAdMutation, useUpdateAdMutation } from "../../api/marketerBannerApi";
import { marketerBannerService } from "../../services/marketerBannerService";
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
    slotIds: editTarget?.slotIds ?? [] as string[],
    title: editTarget?.title ?? "",
    bannerImageUrl: editTarget?.bannerImageUrl ?? "",
    adImageUrl: editTarget?.adImageUrl ?? "",
    linkUrl: editTarget?.linkUrl ?? "",
    priority: editTarget?.priority ?? 1,
    startsAt: editTarget ? toDatetimeLocal(editTarget.startsAt) : "",
    endsAt: editTarget ? toDatetimeLocal(editTarget.endsAt) : "",
  });
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const [uploadingAd, setUploadingAd] = useState(false);
  const [error, setError] = useState("");
  const bannerFileRef = useRef<HTMLInputElement>(null);
  const adFileRef = useRef<HTMLInputElement>(null);

  const isLoading = registering || updating || uploadingBanner || uploadingAd;

  // 선택된 슬롯들의 타입 집합
  const selectedSlots = slots.filter((s: BannerSlot) => form.slotIds.includes(s.id));
  const needsBannerImage = selectedSlots.some((s: BannerSlot) => s.type === "BANNER");
  const needsAdImage = selectedSlots.some((s: BannerSlot) => s.type === "TAB");

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const toggleSlot = (id: string) =>
    setForm((prev) => ({
      ...prev,
      slotIds: prev.slotIds.includes(id)
        ? prev.slotIds.filter((s) => s !== id)
        : [...prev.slotIds, id],
    }));

  const handleFileUpload = async (
    file: File,
    field: "bannerImageUrl" | "adImageUrl",
    setUploading: (v: boolean) => void,
  ) => {
    setUploading(true);
    setError("");
    try {
      const url = await marketerBannerService.uploadImage(file);
      setForm((prev) => ({ ...prev, [field]: url }));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "이미지 업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const payload = {
        ...form,
        bannerImageUrl: form.bannerImageUrl || undefined,
        adImageUrl: form.adImageUrl || undefined,
        priority: Number(form.priority),
        startsAt: toOffsetDateTime(form.startsAt),
        endsAt: toOffsetDateTime(form.endsAt),
      };
      if (isEdit && editTarget) {
        const { slotIds: _, ...updatePayload } = payload;
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

          {/* 슬롯 선택 (등록 시만) */}
          {!isEdit && (
            <div className={styles.field}>
              <label className={styles.label}>광고 슬롯 (복수 선택 가능)</label>
              <div className={styles.checkboxGroup}>
                {slots.map((slot: BannerSlot) => (
                  <label key={slot.id} className={styles.checkboxLabel}>
                    <input
                      type="checkbox"
                      checked={form.slotIds.includes(slot.id)}
                      onChange={() => toggleSlot(slot.id)}
                    />
                    {slot.name}
                    <span className={styles.slotTypeBadge} data-type={slot.type}>
                      {BANNER_SLOT_TYPE_LABEL[slot.type]}
                    </span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* 광고 제목 */}
          <div className={styles.field}>
            <label className={styles.label}>광고 제목</label>
            <input
              className={styles.input}
              value={form.title}
              onChange={set("title")}
              placeholder="광고 제목"
              required
            />
          </div>

          {/* 배너 이미지 (BANNER 슬롯 선택 시) */}
          {(needsBannerImage || (isEdit && editTarget?.bannerImageUrl)) && (
            <div className={styles.field}>
              <label className={styles.label}>배너 이미지</label>
              <div className={styles.fileRow}>
                <input
                  ref={bannerFileRef}
                  type="file"
                  accept="image/*"
                  className={styles.fileInput}
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleFileUpload(file, "bannerImageUrl", setUploadingBanner);
                  }}
                />
                <button
                  type="button"
                  className={styles.btnFile}
                  onClick={() => bannerFileRef.current?.click()}
                  disabled={uploadingBanner}
                >
                  {uploadingBanner ? "업로드 중..." : "파일 선택"}
                </button>
                {form.bannerImageUrl && (
                  <span className={styles.fileOk}>✓ 업로드 완료</span>
                )}
              </div>
              {form.bannerImageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={form.bannerImageUrl} alt="배너 미리보기" className={styles.preview} />
              )}
            </div>
          )}

          {/* 광고 이미지 (TAB 슬롯 선택 시) */}
          {(needsAdImage || (isEdit && editTarget?.adImageUrl)) && (
            <div className={styles.field}>
              <label className={styles.label}>광고탭 이미지</label>
              <div className={styles.fileRow}>
                <input
                  ref={adFileRef}
                  type="file"
                  accept="image/*"
                  className={styles.fileInput}
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleFileUpload(file, "adImageUrl", setUploadingAd);
                  }}
                />
                <button
                  type="button"
                  className={styles.btnFile}
                  onClick={() => adFileRef.current?.click()}
                  disabled={uploadingAd}
                >
                  {uploadingAd ? "업로드 중..." : "파일 선택"}
                </button>
                {form.adImageUrl && (
                  <span className={styles.fileOk}>✓ 업로드 완료</span>
                )}
              </div>
              {form.adImageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={form.adImageUrl} alt="광고탭 이미지 미리보기" className={styles.preview} />
              )}
            </div>
          )}

          {/* 랜딩 URL */}
          <div className={styles.field}>
            <label className={styles.label}>랜딩 URL</label>
            <input
              className={styles.input}
              value={form.linkUrl}
              onChange={set("linkUrl")}
              placeholder="https://..."
              required
            />
          </div>

          {/* 기간 */}
          <div className={styles.row}>
            <div className={styles.field}>
              <label className={styles.label}>시작일시</label>
              <input
                type="datetime-local"
                className={styles.input}
                value={form.startsAt}
                onChange={set("startsAt")}
                required
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>종료일시</label>
              <input
                type="datetime-local"
                className={styles.input}
                value={form.endsAt}
                onChange={set("endsAt")}
                required
              />
            </div>
          </div>

          {/* 우선순위 */}
          <div className={styles.field}>
            <label className={styles.label}>우선순위</label>
            <input
              type="number"
              className={styles.input}
              value={form.priority}
              onChange={set("priority")}
              min={1}
              max={100}
              required
            />
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
