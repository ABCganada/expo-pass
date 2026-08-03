"use client";

import { useRef, useState } from "react";
import type { BannerSlot, MarketerBannerAd } from "../../types/marketerBanner";
import { BANNER_SLOT_TYPE_LABEL } from "../../types/marketerBanner";
import { useGetBannerSlotsQuery, useRegisterAdMutation, useUpdateAdMutation } from "../../api/marketerBannerApi";
import { marketerBannerService } from "../../services/marketerBannerService";
import { PaymentCheckoutButton } from "@/features/payment/components/PaymentCheckoutButton";
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

function calcDurationDays(startsAt: string, endsAt: string): number {
  if (!startsAt || !endsAt) return 0;
  const diff = new Date(endsAt).getTime() - new Date(startsAt).getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

function calcTotalPrice(slots: BannerSlot[], durationDays: number): number {
  return slots.reduce((sum, slot) => sum + slot.pricePerDay * durationDays, 0);
}

function formatPrice(price: number): string {
  return price.toLocaleString("ko-KR") + "원";
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
    startsAt: editTarget ? toDatetimeLocal(editTarget.startsAt) : "",
    endsAt: editTarget ? toDatetimeLocal(editTarget.endsAt) : "",
  });
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const [uploadingAd, setUploadingAd] = useState(false);
  const [error, setError] = useState("");
  const [pendingPayment, setPendingPayment] = useState<{
    orderId: string;
    amount: number;
    title: string;
  } | null>(null);
  const bannerFileRef = useRef<HTMLInputElement>(null);
  const adFileRef = useRef<HTMLInputElement>(null);

  const isLoading = registering || updating || uploadingBanner || uploadingAd;

  const selectedSlots = slots.filter((s) => form.slotIds.includes(s.id));
  const needsBannerImage = selectedSlots.some((s) => s.type === "BANNER");
  const needsAdImage = selectedSlots.some((s) => s.type === "TAB");
  const durationDays = calcDurationDays(form.startsAt, form.endsAt);
  const totalPrice = durationDays > 0 && selectedSlots.length > 0
    ? calcTotalPrice(selectedSlots, durationDays)
    : null;

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  const toggleSlot = (id: string) => {
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

    if (durationDays <= 0) {
      setError("종료일은 시작일보다 이후여야 합니다.");
      return;
    }

    try {
      if (isEdit && editTarget) {
        await updateAd({
          id: editTarget.id,
          title: form.title,
          bannerImageUrl: form.bannerImageUrl || undefined,
          adImageUrl: form.adImageUrl || undefined,
          linkUrl: form.linkUrl,
          startsAt: toOffsetDateTime(form.startsAt),
          endsAt: toOffsetDateTime(form.endsAt),
        }).unwrap();
        onClose();
      } else {
        const created = await registerAd({
          slotIds: form.slotIds,
          title: form.title,
          bannerImageUrl: form.bannerImageUrl || undefined,
          adImageUrl: form.adImageUrl || undefined,
          linkUrl: form.linkUrl,
          startsAt: toOffsetDateTime(form.startsAt),
          endsAt: toOffsetDateTime(form.endsAt),
        }).unwrap();
        setPendingPayment({ orderId: created.orderId, amount: created.totalAmount, title: created.title });
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    }
  };

  if (pendingPayment) {
    return (
      <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
        <div className={styles.modal}>
          <h2 className={styles.title}>광고 결제</h2>
          <p className={styles.paymentDesc}>광고가 등록되었습니다. 결제를 완료해야 검토가 시작됩니다.</p>
          <div className={styles.paymentSummary}>
            <div className={styles.paymentRow}>
              <span>광고 제목</span>
              <strong>{pendingPayment.title}</strong>
            </div>
            <div className={styles.paymentRow}>
              <span>결제 금액</span>
              <strong>{pendingPayment.amount.toLocaleString("ko-KR")}원</strong>
            </div>
          </div>
          <PaymentCheckoutButton
            orderId={pendingPayment.orderId}
            amount={pendingPayment.amount}
            orderName={pendingPayment.title}
            successUrl="/manager/banner-ads/payment/success"
            failUrl="/manager/banner-ads/payment/fail"
          />
          <button type="button" className={styles.btnCancel} onClick={onClose}>
            나중에 결제
          </button>
        </div>
      </div>
    );
  }

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
                    <span className={styles.slotPrice}>{formatPrice(slot.pricePerDay)}/일</span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* 시작일 / 종료일 */}
          <div className={styles.field}>
            <label className={styles.label}>시작일시</label>
            <input type="datetime-local" className={styles.input} value={form.startsAt} onChange={set("startsAt")} required />
          </div>

          <div className={styles.field}>
            <label className={styles.label}>종료일시</label>
            <input type="datetime-local" className={styles.input} value={form.endsAt} onChange={set("endsAt")} required />
            {durationDays > 0 && (
              <div className={styles.endsAtPreview}>
                <span className={styles.endsAtLabel}>{durationDays}일</span>
                {totalPrice !== null && (
                  <span className={styles.totalPrice}>총 {formatPrice(totalPrice)}</span>
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

          {error && <p className={styles.error}>{error}</p>}
          <div className={styles.actions}>
            <button type="button" className={styles.btnCancel} onClick={onClose}>취소</button>
            <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
              {isLoading ? "저장 중..." : isEdit ? "수정" : "등록(결제)"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
