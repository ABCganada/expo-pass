"use client";

import { ImageGridManager } from "../ImageGridManager/ImageGridManager";
import type { EventImageItem } from "../../types/eventManagementDetail";
import styles from "./EventCreateStepper.module.css";

interface Step3ImagesProps {
  eventId: string;
  images: EventImageItem[];
  onBack: () => void;
  onNext: () => void;
}

export function Step3Images({ eventId, images, onBack, onNext }: Step3ImagesProps) {
  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <ImageGridManager eventId={eventId} images={images} mode="manager" />
        <div className={styles.stepActions}>
          <button type="button" className={styles.backButton} onClick={onBack}>
            이전
          </button>
          <button type="button" className={styles.nextButton} onClick={onNext}>
            다음
          </button>
        </div>
      </div>
    </div>
  );
}