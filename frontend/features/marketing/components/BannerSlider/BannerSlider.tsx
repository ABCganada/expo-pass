"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useGetActiveBannersQuery } from "../../api/bannerApi";
import { bannerService } from "../../services/bannerService";
import styles from "./BannerSlider.module.css";

export function BannerSlider() {
  const { data: banners = [], isLoading } = useGetActiveBannersQuery();
  const [currentIndex, setCurrentIndex] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const resetTimer = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % banners.length);
    }, 3000);
  };

  // 배너 로드 후 자동 슬라이드 시작
  useEffect(() => {
    if (banners.length < 2) return;
    resetTimer();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [banners.length]);

  // 슬라이드 변경 시 impression 기록
  useEffect(() => {
    if (banners.length === 0) return;
    bannerService.recordImpression(banners[currentIndex].id);
  }, [currentIndex, banners]);

  const goTo = (index: number) => {
    setCurrentIndex((index + banners.length) % banners.length);
    resetTimer();
  };

  const handleBannerClick = async (id: string, linkUrl: string) => {
    await bannerService.recordClick(id);
    window.open(linkUrl, "_blank", "noopener,noreferrer");
  };

  if (isLoading) return <div className={styles.skeleton} aria-busy="true" />;
  if (banners.length === 0) return null;

  const current = banners[currentIndex];

  return (
    <section className={styles.slider} aria-label="광고 배너">
      <div className={styles.track}>
        <button
          className={styles.banner}
          onClick={() => handleBannerClick(current.id, current.linkUrl)}
          aria-label={`${current.title} 광고 바로가기`}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={current.bannerImageUrl ?? current.adImageUrl ?? ""}
            alt={current.title}
            className={styles.image}
            draggable={false}
          />
          <div className={styles.overlay}>
            <span className={styles.title}>{current.title}</span>
          </div>
        </button>
      </div>

      {banners.length > 1 && (
        <>
          <button
            className={`${styles.arrow} ${styles.arrowLeft}`}
            onClick={() => goTo(currentIndex - 1)}
            aria-label="이전 배너"
          >
            <ChevronLeft size={20} />
          </button>
          <button
            className={`${styles.arrow} ${styles.arrowRight}`}
            onClick={() => goTo(currentIndex + 1)}
            aria-label="다음 배너"
          >
            <ChevronRight size={20} />
          </button>

          <div className={styles.dots} role="tablist" aria-label="배너 목록">
            {banners.map((banner, i) => (
              <button
                key={banner.id}
                role="tab"
                aria-selected={i === currentIndex}
                aria-label={`${i + 1}번 배너`}
                className={`${styles.dot} ${i === currentIndex ? styles.dotActive : ""}`}
                onClick={() => goTo(i)}
              />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
