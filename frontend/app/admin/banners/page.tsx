"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { useGetAdminAllAdsQuery, useGetAdminSlotsQuery } from "@/features/marketing/api/adminBannerApi";
import { AdminAdCard } from "@/features/marketing/components/AdminAdCard/AdminAdCard";
import { CreateSlotModal } from "@/features/marketing/components/CreateSlotModal/CreateSlotModal";
import type { BannerSlot } from "@/features/marketing/types/marketerBanner";
import styles from "./page.module.css";

export default function AdminBannersPage() {
  const { data: ads = [], isLoading: adsLoading } = useGetAdminAllAdsQuery();
  const { data: slots = [], isLoading: slotsLoading } = useGetAdminSlotsQuery();
  const [slotModalOpen, setSlotModalOpen] = useState(false);

  const slotMap = new Map<string, string>(slots.map((s: BannerSlot) => [s.id, s.name]));
  const pendingAds = ads.filter((ad) => ad.status === "PENDING");
  const otherAds = ads.filter((ad) => ad.status !== "PENDING");

  return (
    <div className={styles.page}>
      {/* 슬롯 관리 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>광고 슬롯</h2>
          <button
            type="button"
            className={styles.btnPrimary}
            onClick={() => setSlotModalOpen(true)}
          >
            <Plus size={16} />
            슬롯 생성
          </button>
        </div>

        {slotsLoading && <p className={styles.state}>불러오는 중...</p>}

        {!slotsLoading && slots.length === 0 && (
          <p className={styles.empty}>등록된 슬롯이 없습니다.</p>
        )}

        {!slotsLoading && slots.length > 0 && (
          <div className={styles.slotGrid}>
            {slots.map((slot: BannerSlot) => (
              <div key={slot.id} className={styles.slotCard}>
                <span className={styles.slotName}>{slot.name}</span>
                <span className={styles.slotMeta}>최대 {slot.maxCount}개</span>
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
              <AdminAdCard key={ad.id} ad={ad} slotName={slotMap.get(ad.slotId)} />
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
              <AdminAdCard key={ad.id} ad={ad} slotName={slotMap.get(ad.slotId)} />
            ))}
          </div>
        )}
      </section>

      {slotModalOpen && <CreateSlotModal onClose={() => setSlotModalOpen(false)} />}
    </div>
  );
}
