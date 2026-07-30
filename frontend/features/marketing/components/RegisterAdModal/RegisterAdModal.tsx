"use client";

import { useRef, useState } from "react";
import type { BannerSlotWithPolicies, MarketerBannerAd } from "../../types/marketerBanner";
import { BANNER_SLOT_TYPE_LABEL } from "../../types/marketerBanner";
import { useGetBannerSlotsWithPoliciesQuery, useRegisterAdMutation, useUpdateAdMutation } from "../../api/marketerBannerApi";
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

function addDays(datetimeLocal: string, days: number): string {
  if (!datetimeLocal) return "";
  const date = new Date(datetimeLocal);
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 16);
}

function getCommonDurations(slots: BannerSlotWithPolicies[]): number[] {
  if (slots.length === 0) return [];
  const sets = slots.map((s) => new Set(s.policies.map((p) => p.durationDays)));
  const first = [...sets[0]];
  return first.filter((d) => sets.every((s) => s.has(d))).sort((a, b) => a - b);
}

function calcTotalPrice(slots: BannerSlotWithPolicies[], durationDays: number): number {
  return slots.reduce((sum, slot) => {
    const policy = slot.policies.find((p) => p.durationDays === durationDays);
    return sum + (policy?.price ?? 0);
  }, 0);
}

function formatPrice(price: number): string {
  return price.toLocaleString("ko-KR") + "원";
}

export function RegisterAdModal({ editTarget, onClose }: RegisterAdModalProps) {
  const isEdit = !!editTarget;
  const { data: slotsWithPolicies = [] } = useGetBannerSlotsWithPoliciesQuery();
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
  const [selectedDuration, setSelectedDuration] = useState<number | null>(null);
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const [uploadingAd, setUploadingAd] = useState(false);
  const [error, setError] = useState("");
  const bannerFileRef = useRef<HTMLInputElement>(null);
  const adFileRef = useRef<HTMLInputElement>(null);

  const isLoading = registering || updating || uploadingBanner || uploadingAd;

  const selectedSlots = slotsWithPolicies.filter((s) => form.slotIds.includes(s.id));
  const needsBannerImage = selectedSlots.some((s) => s.type === "BANNER");
  const needsAdImage = selectedSlots.some((s) => s.type === "TAB");
  const commonDurations = getCommonDurations(selectedSlots);
  const computedEndsAt = !isEdit && selectedDuration && form.startsAt
    ? addDays(form.startsAt, selectedDuration)
    : form.endsAt;
  const totalPrice = !isEdit && selectedDuration
    ? calcTotalPrice(selectedSlots, selectedDuration)
    : null;

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const toggleSlot = (id: string) => {
    setSelectedDuration(null);
    setForm((prev) => ({
      ...prev,
      slotIds: prev.slotIds.includes(id)
        ? prev.slotIds.filter((s) => s !== id)
        : [...prev.slotIds, id],
    }));
  };

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

    if (!isEdit && !selectedDuration) {
      setError("광고 기간을 선택해주세요.");
      return;
    }

    try {
      if (isEdit && editTarget) {
        const payload = {
          title: form.title,
          bannerImageUrl: form.bannerImageUrl || undefined,
          adImageUrl: form.adImageUrl || undefined,
          linkUrl: form.linkUrl,
          priority: Number(form.priority),
          startsAt: toOffsetDateTime(form.startsAt),
          endsAt: toOffsetDateTime(form.endsAt),
        };
        await updateAd({ id: editTarget.id, ...payload }).unwrap();
      } else {
        const endsAt = addDays(form.startsAt, selectedDuration!);
        const payload = {
          slotIds: form.slotIds,
          title: form.title,
          bannerImageUrl: form.bannerImageUrl || undefined,
          adImageUrl: form.adImageUrl || undefined,
          linkUrl: form.linkUrl,
          priority: Number(form.priority),
          startsAt: toOffsetDateTime(form.startsAt),
          endsAt: toOffsetDateTime(endsAt),
        };
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
                {slotsWithPolicies.map((slot: BannerSlotWithPolicies) => (
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

          {/* 광고 기간 선택 (등록 시만, 슬롯 선택 후) */}
          {!isEdit && form.slotIds.length > 0 && (
            <div className={styles.field}>
              <label className={styles.label}>광고 기간</label>
              {commonDurations.length === 0 ? (
                <p className={styles.noPolicyMsg}>선택한 슬롯 조합에 공통 가격 정책이 없습니다.</p>
              ) : (
                <div className={styles.durationGroup}>
                  {commonDurations.map((days) => {
                    const price = calcTotalPrice(selectedSlots, days);
                    return (
                      <label
                        key={days}
                        className={`${styles.durationOption} ${selectedDuration === days ? styles.durationOptionSelected : ""}`}
                      >
                        <input
                          type="radio"
                          name="duration"
                          value={days}
                          checked={selectedDuration === days}
                          onChange={() => setSelectedDuration(days)}
                          className={styles.radioHidden}
                        />
                        <span className={styles.durationDays}>{days}일</span>
                        <span className={styles.durationPrice}>{formatPrice(price)}</span>
                      </label>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* 시작일 */}
          <div className={styles.field}>
            <label className={styles.label}>시작일시</label>
            {isEdit ? (
              <div className={styles.row}>
                <input type="datetime-local" className={styles.input} value={form.startsAt} onChange={set("startsAt")} required />
                <input type="datetime-local" className={styles.input} value={form.endsAt} onChange={set("endsAt")} required />
              </div>
            ) : (
              <div className={styles.dateRow}>
                <input type="datetime-local" className={styles.input} value={form.startsAt} onChange={set("startsAt")} required />
                {selectedDuration && computedEndsAt && (
                  <div className={styles.endsAtPreview}>
                    <span className={styles.endsAtLabel}>종료</span>
                    <span>{new Date(computedEndsAt).toLocaleDateString("ko-KR")}</span>
                    {totalPrice !== null && (
                      <span className={styles.totalPrice}>{formatPrice(totalPrice)}</span>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* 배너 이미지 */}
          {(needsBannerImage || (isEdit && editTarget?.bannerImageUrl)) && (
            <div className={styles.field}>
              <label className={styles.label}>배너 이미지</label>
              <div className={styles.fileRow}>
                <input ref={bannerFileRef} type="file" accept="image/*" className={styles.fileInput}
                  onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFileUpload(f, "bannerImageUrl", setUploadingBanner); }} />
                <button type="button" className={styles.btnFile} onClick={() => bannerFileRef.current?.click()} disabled={uploadingBanner}>
                  {uploadingBanner ? "업로드 중..." : "파일 선택"}
                </button>
                {form.bannerImageUrl && <span className={styles.fileOk}>✓ 업로드 완료</span>}
              </div>
              {form.bannerImageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={form.bannerImageUrl} alt="배너 미리보기" className={styles.preview} />
              )}
            </div>
          )}

          {/* 광고탭 이미지 */}
          {(needsAdImage || (isEdit && editTarget?.adImageUrl)) && (
            <div className={styles.field}>
              <label className={styles.label}>광고탭 이미지</label>
              <div className={styles.fileRow}>
                <input ref={adFileRef} type="file" accept="image/*" className={styles.fileInput}
                  onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFileUpload(f, "adImageUrl", setUploadingAd); }} />
                <button type="button" className={styles.btnFile} onClick={() => adFileRef.current?.click()} disabled={uploadingAd}>
                  {uploadingAd ? "업로드 중..." : "파일 선택"}
                </button>
                {form.adImageUrl && <span className={styles.fileOk}>✓ 업로드 완료</span>}
              </div>
              {form.adImageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={form.adImageUrl} alt="광고탭 이미지 미리보기" className={styles.preview} />
              )}
            </div>
          )}

          {/* 광고 제목 */}
          <div className={styles.field}>
            <label className={styles.label}>광고 제목</label>
            <input className={styles.input} value={form.title} onChange={set("title")} placeholder="광고 제목" required />
          </div>

          {/* 랜딩 URL */}
          <div className={styles.field}>
            <label className={styles.label}>랜딩 URL</label>
            <input className={styles.input} value={form.linkUrl} onChange={set("linkUrl")} placeholder="https://..." required />
          </div>

          {/* 우선순위 */}
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
