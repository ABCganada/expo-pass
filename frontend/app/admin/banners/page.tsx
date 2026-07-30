"use client";

import { useState } from "react";
import { Edit2, Plus, Trash2 } from "lucide-react";
import {
  useDeleteSlotMutation,
  useGetAdminAllAdsQuery,
  useGetAdminSlotsQuery,
} from "@/features/marketing/api/adminBannerApi";
import { AdminAdCard } from "@/features/marketing/components/AdminAdCard/AdminAdCard";
import { CreateSlotModal } from "@/features/marketing/components/CreateSlotModal/CreateSlotModal";
import type { BannerSlot } from "@/features/marketing/types/marketerBanner";
import styles from "./page.module.css";

export default function AdminBannersPage() {
  const { data: ads = [], isLoading: adsLoading } = useGetAdminAllAdsQuery();
  const { data: slots = [], isLoading: slotsLoading } = useGetAdminSlotsQuery();
  const [deleteSlot] = useDeleteSlotMutation();

  const [slotModalTarget, setSlotModalTarget] = useState<BannerSlot | null | "new">(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const slotMap = new Map<string, string>(slots.map((s: BannerSlot) => [s.id, s.name]));
  const pendingAds = ads.filter((ad) => ad.status === "PENDING");
  const otherAds = ads.filter((ad) => ad.status !== "PENDING");

  const handleDeleteSlot = async (slot: BannerSlot) => {
    if (!confirm(`"${slot.name}" 슬롯을 삭제하시겠습니까?`)) return;
    setDeleteError(null);
    try {
      await deleteSlot(slot.id).unwrap();
    } catch (err: unknown) {
      setDeleteError(err instanceof Error ? err.message : "슬롯 삭제에 실패했습니다.");
    }
  };

  return (
    <div className={styles.page}>
      {/* 슬롯 관리 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>광고 슬롯</h2>
          <button
            type="button"
            className={styles.btnPrimary}
            onClick={() => setSlotModalTarget("new")}
          >
            <Plus size={16} />
            슬롯 생성
          </button>
        </div>

        {slotsLoading && <p className={styles.state}>불러오는 중...</p>}

        {deleteError && <p className={styles.errorMsg}>{deleteError}</p>}

        {!slotsLoading && slots.length === 0 && (
          <p className={styles.empty}>등록된 슬롯이 없습니다.</p>
        )}

        {!slotsLoading && slots.length > 0 && (
          <div className={styles.slotGrid}>
            {slots.map((slot: BannerSlot) => (
              <div key={slot.id} className={styles.slotCard}>
                <div className={styles.slotInfo}>
                  <span className={styles.slotName}>{slot.name}</span>
                  <span className={styles.slotMeta}>
                    {slot.type === "BANNER" ? "배너형" : "광고탭형"} · 최대 {slot.maxCount}개
                  </span>
                </div>
                <div className={styles.slotActions}>
                  <button
                    type="button"
                    className={styles.btnIconEdit}
                    onClick={() => setSlotModalTarget(slot)}
                    aria-label="슬롯 수정"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button
                    type="button"
                    className={styles.btnIconDelete}
                    onClick={() => handleDeleteSlot(slot)}
                    aria-label="슬롯 삭제"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 광고 승인 대기 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>승인 대기</h2>
          {pendingAds.length > 0 && (
            <span className={styles.countBadge}>{pendingAds.length}</span>
          )}
        </div>

        {adsLoading && <p className={styles.state}>불러오는 중...</p>}

        {!adsLoading && pendingAds.length === 0 && (
          <p className={styles.empty}>승인 대기 중인 광고가 없습니다.</p>
        )}

        {!adsLoading && pendingAds.length > 0 && (
          <div className={styles.adGrid}>
            {pendingAds.map((ad) => (
              <AdminAdCard key={ad.id} ad={ad} slotName={ad.slotIds.map((id) => slotMap.get(id) ?? id).join(", ")} />
            ))}
          </div>
        )}
      </section>

      {/* 전체 광고 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>전체 광고</h2>
        </div>

        {!adsLoading && otherAds.length === 0 && (
          <p className={styles.empty}>광고가 없습니다.</p>
        )}

        {!adsLoading && otherAds.length > 0 && (
          <div className={styles.adGrid}>
            {otherAds.map((ad) => (
              <AdminAdCard key={ad.id} ad={ad} slotName={ad.slotIds.map((id) => slotMap.get(id) ?? id).join(", ")} />
            ))}
          </div>
        )}
      </section>

      {slotModalTarget && (
        <CreateSlotModal
          editTarget={slotModalTarget === "new" ? undefined : slotModalTarget}
          onClose={() => setSlotModalTarget(null)}
        />
      )}
    </div>
  );
}
