"use client";

import { useState } from "react";
import {
  useGetAdminAllAdsQuery,
  useGetAdminSlotsQuery,
} from "@/features/marketing/api/adminBannerApi";
import { AdminAdCard } from "@/features/marketing/components/AdminAdCard/AdminAdCard";
import { BANNER_SLOT_TYPE_LABEL } from "@/features/marketing/types/marketerBanner";
import type { BannerSlot, MarketerBannerAd } from "@/features/marketing/types/marketerBanner";
import styles from "./page.module.css";

type SortOrder = "deadline" | "created";

export default function AdminBannersPage() {
  const { data: ads = [], isLoading: adsLoading } = useGetAdminAllAdsQuery();
  const { data: slots = [], isLoading: slotsLoading } = useGetAdminSlotsQuery();

  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null);
  const [sortOrder, setSortOrder] = useState<SortOrder>("deadline");

  const slotMap = new Map<string, string>(slots.map((s: BannerSlot) => [s.id, s.name]));
  const pendingAds = ads.filter((ad) => ad.status === "PENDING");
  const otherAds = ads.filter((ad) => ad.status !== "PENDING");

  const slotAds = selectedSlotId
    ? [...ads.filter((ad) => ad.slotIds.includes(selectedSlotId))].sort((a, b) => {
        if (sortOrder === "deadline") return new Date(a.endsAt).getTime() - new Date(b.endsAt).getTime();
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      })
    : [];

  return (
    <div className={styles.page}>
      {/* 슬롯 탭 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>광고 슬롯</h2>
        </div>

        {slotsLoading && <p className={styles.state}>불러오는 중...</p>}

        {!slotsLoading && slots.length > 0 && (
          <div className={styles.slotTabs}>
            {slots.map((slot: BannerSlot) => (
              <button
                key={slot.id}
                type="button"
                className={`${styles.slotTab} ${selectedSlotId === slot.id ? styles.slotTabActive : ""}`}
                onClick={() => setSelectedSlotId(selectedSlotId === slot.id ? null : slot.id)}
              >
                <span className={styles.slotTabName}>{slot.name}</span>
                <span className={styles.slotTabMeta}>
                  {BANNER_SLOT_TYPE_LABEL[slot.type]} · {slot.pricePerDay.toLocaleString("ko-KR")}원/일
                </span>
              </button>
            ))}
          </div>
        )}

        {/* 슬롯 선택 시 해당 광고 목록 */}
        {selectedSlotId && (
          <div className={styles.slotAdsPanel}>
            <div className={styles.slotAdsPanelHeader}>
              <span className={styles.slotAdsTitle}>진행 중인 광고</span>
              <div className={styles.sortBtns}>
                <button
                  type="button"
                  className={`${styles.sortBtn} ${sortOrder === "deadline" ? styles.sortBtnActive : ""}`}
                  onClick={() => setSortOrder("deadline")}
                >
                  마감순
                </button>
                <button
                  type="button"
                  className={`${styles.sortBtn} ${sortOrder === "created" ? styles.sortBtnActive : ""}`}
                  onClick={() => setSortOrder("created")}
                >
                  등록순
                </button>
              </div>
            </div>

            {slotAds.length === 0 ? (
              <p className={styles.empty}>이 슬롯에 등록된 광고가 없습니다.</p>
            ) : (
              <div className={styles.slotAdsList}>
                {slotAds.map((ad: MarketerBannerAd) => (
                  <div key={ad.id} className={styles.slotAdItem}>
                    <span className={styles.slotAdTitle}>{ad.title}</span>
                    <span className={styles.slotAdDeadline}>
                      ~{new Date(ad.endsAt).toLocaleDateString("ko-KR")}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      {/* 승인 대기 */}
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
    </div>
  );
}
