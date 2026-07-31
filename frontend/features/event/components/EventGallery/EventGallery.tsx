"use client";

import { useState } from "react";
import { Building2, ChevronLeft, ChevronRight } from "lucide-react";
import type { EventImageItem } from "../../types/event";
import styles from "./EventGallery.module.css";

interface EventGalleryProps {
  images: EventImageItem[];
  title: string;
}

export function EventGallery({ images, title }: EventGalleryProps) {
  const slides = [...images].sort((a, b) => a.displayOrder - b.displayOrder);
  const [index, setIndex] = useState(0);

  if (slides.length === 0) {
    return (
      <div className={styles.gallery}>
        <div className={styles.empty}>
          <Building2 size={48} className={styles.emptyIcon} />
        </div>
      </div>
    );
  }

  const goTo = (next: number) => setIndex((next + slides.length) % slides.length);

  return (
    <div className={styles.gallery}>
      <div className={styles.frame}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={slides[index].imageUrl} alt={`${title} 이미지 ${index + 1}`} className={styles.image} draggable={false} />

        {slides.length > 1 && (
          <>
            <button
              type="button"
              className={`${styles.navButton} ${styles.navPrev}`}
              aria-label="이전 이미지"
              onClick={() => goTo(index - 1)}
            >
              <ChevronLeft size={20} />
            </button>
            <button
              type="button"
              className={`${styles.navButton} ${styles.navNext}`}
              aria-label="다음 이미지"
              onClick={() => goTo(index + 1)}
            >
              <ChevronRight size={20} />
            </button>
          </>
        )}
      </div>

      {slides.length > 1 && (
        <div className={styles.dots} role="tablist" aria-label="이미지 선택">
          {slides.map((slide, i) => (
            <button
              key={slide.id}
              type="button"
              role="tab"
              aria-selected={i === index}
              aria-label={`${i + 1}번째 이미지`}
              className={styles.dot}
              data-active={i === index}
              onClick={() => setIndex(i)}
            />
          ))}
        </div>
      )}
    </div>
  );
}