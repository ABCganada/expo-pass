"use client";

import { Megaphone } from "lucide-react";
import { useGetActiveBannersQuery } from "../../api/bannerApi";
import { VipAdCard } from "../VipAdCard";
import styles from "./VipAdsContent.module.css";

export function VipAdsContent() {
  const { data: allBanners = [], isLoading } = useGetActiveBannersQuery();
  const banners = allBanners.filter((b) =>
    b.slotTypes?.length ? b.slotTypes.includes("TAB") : !!b.adImageUrl,
  );

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>VIP 광고</h1>
        <p className={styles.subtitle}>엄선된 프리미엄 광고 파트너를 만나보세요.</p>
      </header>

      {isLoading && (
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className={styles.skeleton} aria-busy="true" />
          ))}
        </div>
      )}

      {!isLoading && banners.length === 0 && (
        <div className={styles.empty}>
          <Megaphone size={48} className={styles.emptyIcon} />
          <p className={styles.emptyText}>현재 진행 중인 광고가 없습니다.</p>
        </div>
      )}

      {!isLoading && banners.length > 0 && (
        <div className={styles.grid}>
          {banners.map((ad) => (
            <VipAdCard key={ad.id} ad={ad} />
          ))}
        </div>
      )}
    </div>
  );
}
