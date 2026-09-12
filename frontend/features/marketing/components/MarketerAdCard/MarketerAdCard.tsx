"use client";

import { BarChart2, CalendarRange, CreditCard, Edit2, Link, Trash2 } from "lucide-react";
import { useDeleteMyAdMutation } from "../../api/marketerBannerApi";
import type { MarketerBannerAd } from "../../types/marketerBanner";
import { BANNER_AD_STATUS_LABEL } from "../../types/marketerBanner";
import styles from "./MarketerAdCard.module.css";

interface MarketerAdCardProps {
  ad: MarketerBannerAd;
  slotName?: string;
  onEdit: (ad: MarketerBannerAd) => void;
  onPay: (ad: MarketerBannerAd) => void;
  onStats: (ad: MarketerBannerAd) => void;
  onPaymentDetail: (ad: MarketerBannerAd) => void;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export function MarketerAdCard({ ad, slotName, onEdit, onPay, onStats, onPaymentDetail }: MarketerAdCardProps) {
  const [deleteAd, { isLoading: isDeleting }] = useDeleteMyAdMutation();

  const handleDelete = async () => {
    if (!window.confirm("광고를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) return;
    await deleteAd(ad.id);
  };

  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <span className={styles.title}>{ad.title}</span>
        <span className={styles.badge} data-status={ad.status}>
          {BANNER_AD_STATUS_LABEL[ad.status]}
        </span>
      </div>

      <div className={styles.meta}>
        {slotName && (
          <div className={styles.metaRow}>
            <Link size={13} />
            <span>{slotName}</span>
          </div>
        )}
        <div className={styles.metaRow}>
          <CalendarRange size={13} />
          <span>
            {formatDate(ad.startsAt)} ~ {formatDate(ad.endsAt)}
          </span>
        </div>
      </div>

      <div className={styles.actions}>
        {ad.status === "PENDING" && (
          <>
            <button type="button" className={styles.btnOutline} onClick={() => onEdit(ad)}>
              <Edit2 size={14} />
              수정
            </button>
            <button type="button" className={styles.btnPrimary} onClick={() => onPay(ad)}>
              <CreditCard size={14} />
              결제하기
            </button>
            <button
              type="button"
              className={styles.btnDanger}
              onClick={handleDelete}
              disabled={isDeleting}
            >
              <Trash2 size={14} />
              삭제
            </button>
          </>
        )}
        {ad.status !== "PENDING" && (
          <>
            <button type="button" className={styles.btnOutline} onClick={() => onPaymentDetail(ad)}>
              <CreditCard size={14} />
              결제 정보
            </button>
            <button type="button" className={styles.btnPrimary} onClick={() => onStats(ad)}>
              <BarChart2 size={14} />
              성과 보기
            </button>
          </>
        )}
      </div>
    </div>
  );
}
