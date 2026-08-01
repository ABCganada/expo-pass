"use client";

import { useGetActiveBannersQuery } from "../../api/bannerApi";
import { bannerService } from "../../services/bannerService";
import styles from "./TabAdSection.module.css";

export function TabAdSection() {
  const { data: allBanners = [] } = useGetActiveBannersQuery();
  const tabAds = allBanners.filter((b) => !!b.adImageUrl);

  if (tabAds.length === 0) return null;

  const handleClick = async (id: string, linkUrl: string) => {
    await bannerService.recordClick(id);
    const url = /^https?:\/\//i.test(linkUrl) ? linkUrl : `https://${linkUrl}`;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  return (
    <section className={styles.section} aria-label="광고 탭">
      <div className={styles.row}>
        {tabAds.map((ad) => (
          <button
            key={ad.id}
            className={styles.tab}
            onClick={() => handleClick(ad.id, ad.linkUrl)}
            aria-label={`${ad.title} 광고 바로가기`}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={ad.adImageUrl} alt={ad.title} className={styles.image} draggable={false} />
            <span className={styles.title}>{ad.title}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
