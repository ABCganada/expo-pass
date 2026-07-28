"use client";

import { useEffect } from "react";
import { ExternalLink } from "lucide-react";
import { bannerService } from "../../services/bannerService";
import type { BannerAd } from "../../types/banner";
import styles from "./VipAdCard.module.css";

interface VipAdCardProps {
  ad: BannerAd;
}

export function VipAdCard({ ad }: VipAdCardProps) {
  useEffect(() => {
    bannerService.recordImpression(ad.id);
  }, [ad.id]);

  const handleClick = async () => {
    await bannerService.recordClick(ad.id);
    window.open(ad.linkUrl, "_blank", "noopener,noreferrer");
  };

  return (
    <article className={styles.card}>
      <div className={styles.imageWrapper}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={ad.imageUrl} alt={ad.title} className={styles.image} draggable={false} />
      </div>
      <div className={styles.body}>
        <p className={styles.title}>{ad.title}</p>
        <button
          className={styles.linkButton}
          onClick={handleClick}
          aria-label={`${ad.title} 광고 바로가기`}
        >
          <ExternalLink size={16} />
          바로가기
        </button>
      </div>
    </article>
  );
}
