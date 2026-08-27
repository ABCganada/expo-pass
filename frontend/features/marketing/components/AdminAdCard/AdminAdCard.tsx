"use client";

import { useEffect, useRef, useState } from "react";
import { CalendarRange, Check, MoreVertical, Trash2, X } from "lucide-react";
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
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <span className={styles.title}>{ad.title}</span>
          {slotName && <span className={styles.slot}>{slotName}</span>}
        </div>
        <div className={styles.headerRight}>
          <span className={styles.badge} data-status={ad.status}>
            {BANNER_AD_STATUS_LABEL[ad.status]}
          </span>
          <div className={styles.menuWrapper} ref={menuRef}>
            <button
              type="button"
              className={styles.btnMenu}
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="더보기"
              disabled={isActing}
            >
              <MoreVertical size={16} />
            </button>
            {menuOpen && (
              <div className={styles.menu}>
                <button
                  type="button"
                  className={styles.menuItemDanger}
                  onClick={() => { setMenuOpen(false); deleteAd(ad.id); }}
                  disabled={isActing}
                >
                  <Trash2 size={14} />
                  삭제
                </button>
              </div>
            )}
          </div>
        </div>
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

      {ad.status === "PAID" && (
        <div className={styles.actions}>
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
        </div>
      )}
    </div>
  );
}
