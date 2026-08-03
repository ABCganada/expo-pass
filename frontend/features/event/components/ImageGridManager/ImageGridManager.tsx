"use client";

import { useRef, useState } from "react";
import { ImagePlus, X } from "lucide-react";
import { useUploadAdminEventImageMutation, useDeleteAdminEventImageMutation } from "../../api/adminEventDetailApi";
import type { AdminEventImageItem } from "../../types/adminEventDetail";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./ImageGridManager.module.css";

interface ImageGridManagerProps {
  eventId: string;
  images: AdminEventImageItem[];
}

export function ImageGridManager({ eventId, images }: ImageGridManagerProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadImage, { isLoading: isUploading }] = useUploadAdminEventImageMutation();
  const [deleteImage, { isLoading: isDeleting }] = useDeleteAdminEventImageMutation();
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  const handleFilesSelected = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []).filter(
        (file) => file.type.startsWith("image/")
    );

    if (files.length === 0) {
      setError("이미지 파일만 업로드할 수 있습니다.");
      return;
    }

    event.target.value = "";
    setError(null);

    // 첫 장을 대표 이미지로 지정
    let hasThumbnail = images.some((image) => image.imageType === "THUMBNAIL");
    for (const file of files) {
      const imageType = hasThumbnail ? "GENERAL" : "THUMBNAIL";
      try {
        await uploadImage({ eventId, file, imageType }).unwrap();
        if (imageType === "THUMBNAIL") hasThumbnail = true;
      } catch (reason) {
        setError(queryErrorMessage(reason, "이미지 업로드에 실패했습니다."));
      }
    }
  };

  const confirmDelete = async () => {
    if (!pendingDeleteId) return;
    try {
      await deleteImage({ eventId, imageId: pendingDeleteId }).unwrap();
    } catch (reason) {
      setError(queryErrorMessage(reason, "이미지 삭제에 실패했습니다."));
    } finally {
      setPendingDeleteId(null);
    }
  };

  return (
    <div className={styles.manager}>
      <button
        type="button"
        className={styles.uploadButton}
        onClick={() => fileInputRef.current?.click()}
        disabled={isUploading}
      >
        <ImagePlus size={16} />
        {isUploading ? "업로드 중..." : "+ 이미지 업로드"}
      </button>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => void handleFilesSelected(event)}
      />

      {error && <p className={styles.error}>{error}</p>}

      {images.length === 0 ? (
        <p className={styles.empty}>업로드된 이미지가 없습니다.</p>
      ) : (
        <div className={styles.grid}>
          {images.map((image) => (
            <div key={image.id} className={styles.thumb}>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={image.imageUrl} alt="" className={styles.image} />
              {image.imageType === "THUMBNAIL" && <span className={styles.thumbnailBadge}>대표</span>}
              <button
                type="button"
                className={styles.removeButton}
                aria-label="이미지 삭제"
                disabled={isDeleting}
                onClick={() => setPendingDeleteId(image.id)}
              >
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      {pendingDeleteId && (
        <ConfirmDialog
          title="이미지를 삭제할까요?"
          description="삭제하면 되돌릴 수 없습니다."
          confirmLabel="삭제"
          danger
          onConfirm={() => void confirmDelete()}
          onCancel={() => setPendingDeleteId(null)}
        />
      )}
    </div>
  );
}