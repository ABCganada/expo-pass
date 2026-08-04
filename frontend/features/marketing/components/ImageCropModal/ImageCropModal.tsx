"use client";

import { useCallback, useRef, useState, type MouseEvent } from "react";
import ReactCrop, { type Crop, centerCrop, makeAspectCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import styles from "./ImageCropModal.module.css";

interface ImageCropModalProps {
  src: string;
  aspectRatio: number;         // targetWidth / targetHeight
  targetWidth: number;
  targetHeight: number;
  onConfirm: (blob: Blob) => void;
  onCancel: () => void;
}

export function ImageCropModal({
  src,
  aspectRatio,
  targetWidth,
  targetHeight,
  onConfirm,
  onCancel,
}: ImageCropModalProps) {
  const imgRef = useRef<HTMLImageElement>(null);
  const [crop, setCrop] = useState<Crop>();
  const mouseDownTargetRef = useRef<EventTarget | null>(null);

  const onImageLoad = useCallback((e: React.SyntheticEvent<HTMLImageElement>) => {
    const { width, height } = e.currentTarget;
    setCrop(
      centerCrop(
        makeAspectCrop({ unit: "%", width: 90 }, aspectRatio, width, height),
        width,
        height,
      ),
    );
  }, [aspectRatio]);

  const handleConfirm = () => {
    const img = imgRef.current;
    if (!img || !crop || crop.width === 0) return;

    const scaleX = img.naturalWidth / img.width;
    const scaleY = img.naturalHeight / img.height;

    const pixelCrop = {
      x: Math.round((crop.unit === "%" ? (crop.x / 100) * img.width : crop.x) * scaleX),
      y: Math.round((crop.unit === "%" ? (crop.y / 100) * img.height : crop.y) * scaleY),
      width: Math.round((crop.unit === "%" ? (crop.width / 100) * img.width : crop.width) * scaleX),
      height: Math.round((crop.unit === "%" ? (crop.height / 100) * img.height : crop.height) * scaleY),
    };

    const canvas = document.createElement("canvas");
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // cover 방식: 크롭 영역이 캔버스를 항상 꽉 채우도록 스케일 보정
    const coverScale = Math.max(targetWidth / pixelCrop.width, targetHeight / pixelCrop.height);
    const drawW = Math.round(pixelCrop.width * coverScale);
    const drawH = Math.round(pixelCrop.height * coverScale);
    const drawX = Math.round((targetWidth - drawW) / 2);
    const drawY = Math.round((targetHeight - drawH) / 2);

    ctx.drawImage(img, pixelCrop.x, pixelCrop.y, pixelCrop.width, pixelCrop.height, drawX, drawY, drawW, drawH);

    canvas.toBlob((blob) => {
      if (blob) onConfirm(blob);
    }, "image/jpeg", 0.92);
  };

  const handleOverlayMouseDown = (e: MouseEvent<HTMLDivElement>) => {
    mouseDownTargetRef.current = e.target;
  };

  const handleOverlayClick = (e: MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget && mouseDownTargetRef.current === e.currentTarget) {
      onCancel();
    }
  };

  return (
    <div
      className={styles.overlay}
      onMouseDown={handleOverlayMouseDown}
      onClick={handleOverlayClick}
    >
      <div className={styles.modal}>
        <h3 className={styles.title}>이미지 영역 선택</h3>
        <p className={styles.desc}>
          드래그로 위치를 조절하세요. 선택 영역이 {targetWidth}×{targetHeight}px로 저장됩니다.
        </p>
        <div className={styles.cropArea}>
          <ReactCrop
            crop={crop}
            onChange={(c) => setCrop(c)}
            aspect={aspectRatio}
            minWidth={50}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              ref={imgRef}
              src={src}
              alt="크롭 대상 이미지"
              className={styles.cropImg}
              onLoad={onImageLoad}
            />
          </ReactCrop>
        </div>
        <div className={styles.actions}>
          <button type="button" className={styles.btnCancel} onClick={onCancel}>
            취소
          </button>
          <button type="button" className={styles.btnConfirm} onClick={handleConfirm}>
            이 영역으로 저장
          </button>
        </div>
      </div>
    </div>
  );
}
