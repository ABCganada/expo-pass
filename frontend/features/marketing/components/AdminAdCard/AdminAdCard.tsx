"use client";

import { CalendarRange, Check, Trash2, X } from "lucide-react";
import type { MarketerBannerAd } from "../../types/marketerBanner";
import { BANNER_AD_STATUS_LABEL } from "../../types/marketerBanner";
import { useApproveAdMutation, useDeleteAdMutation, useRejectAdMutation } from "../../api/adminBannerApi";
import styles from "./AdminAdCard.module.css";

interface AdminAdCardProps {
  ad: MarketerBannerAd;
  slotName?: string;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export function AdminAdCard({ ad, slotName }: AdminAdCardProps) {
  const [approveAd, { isLoading: approving }] = useApproveAdMutation();
  const [rejectAd, { isLoading: rejecting }] = useRejectAdMutation();
  const [deleteAd, { isLoading: deleting }] = useDeleteAdMutation();
  const isActing = approving || rejecting || deleting;

  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <span className={styles.title}>{ad.title}</span>
          {slotName && <span className={styles.slot}>{slotName}</span>}
        </div>
        <span className={styles.badge} data-status={ad.status}>
          {BANNER_AD_STATUS_LABEL[ad.status]}
        </span>
      </div>

      <div className={styles.meta}>
        <div className={styles.metaRow}>
          <CalendarRange size={13} />
          <span>{formatDate(ad.startsAt)} ~ {formatDate(ad.endsAt)}</span>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>등록자</span>
          <span>{ad.createdBy}</span>
        </div>
      </div>

      <div className={styles.actions}>
        {ad.status === "PENDING" && (
          <>
            <button
              type="button"
              className={styles.btnReject}
              onClick={() => rejectAd(ad.id)}
              disabled={isActing}
            >
              <X size={14} />
              거절
            </button>
            <button
              type="button"
              className={styles.btnApprove}
              onClick={() => approveAd(ad.id)}
              disabled={isActing}
            >
              <Check size={14} />
              승인
            </button>
          </>
        )}
        <button
          type="button"
          className={styles.btnDelete}
          onClick={() => deleteAd(ad.id)}
          disabled={isActing}
        >
          <Trash2 size={14} />
          삭제
        </button>
      </div>
    </div>
  );
}
