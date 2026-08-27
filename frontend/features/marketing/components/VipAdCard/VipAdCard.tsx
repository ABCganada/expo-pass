"use client";

import { useEffect, useRef } from "react";
import { ExternalLink } from "lucide-react";
import { bannerService } from "../../services/bannerService";
import type { BannerAd } from "../../types/banner";
import styles from "./VipAdCard.module.css";

interface VipAdCardProps {
  ad: BannerAd;
}

export function VipAdCard({ ad }: VipAdCardProps) {
  const cardRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const el = cardRef.current;
    if (!el) return;

    // 광고가 뷰포트에 50% 이상 노출됐을 때 한 번만 카운트
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          bannerService.recordImpression(ad.id);
          observer.disconnect();
        }
      },
      { threshold: 0.5 },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [ad.id]);

  const handleClick = async () => {
    await bannerService.recordClick(ad.id);
    const url = /^https?:\/\//i.test(ad.linkUrl) ? ad.linkUrl : `https://${ad.linkUrl}`;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  return (
    <article ref={cardRef} className={styles.card}>
      <div className={styles.imageWrapper}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={ad.adImageUrl ?? ad.bannerImageUrl ?? ""} alt={ad.title} className={styles.image} draggable={false} />
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
