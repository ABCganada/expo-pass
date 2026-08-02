"use client";

import { useEffect, useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { useUpdateAdminEventMutation } from "../../api/adminEventDetailApi";
import type { AdminEventDetail } from "../../types/adminEventDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminEventDetail.module.css";

interface BasicInfoTabProps {
  eventId: string;
  detail: AdminEventDetail;
  onSaved: (message: string) => void;
}

export function BasicInfoTab({ eventId, detail, onSaved }: BasicInfoTabProps) {
  const { data: categories = [] } = useGetEventCategoriesQuery();
  const [updateEvent, { isLoading }] = useUpdateAdminEventMutation();

  const [title, setTitle] = useState(detail.title);
  const [hostName, setHostName] = useState(detail.hostName ?? "");
  const [categoryId, setCategoryId] = useState("");
  const [startDate, setStartDate] = useState(detail.startDate ?? "");
  const [endDate, setEndDate] = useState(detail.endDate ?? "");
  const [venueName, setVenueName] = useState(detail.venueName ?? "");
  const [address, setAddress] = useState(detail.address ?? "");
  const [detailAddress, setDetailAddress] = useState(detail.detailAddress ?? "");
  const [error, setError] = useState<string | null>(null);

  // 상세 응답엔 categoryName만 내려오므로, 카테고리 목록이 로드되면 이름으로 매칭해 초기 선택값을 정한다.
  useEffect(() => {
    if (categoryId || categories.length === 0) return;
    const matched = categories.find((category) => category.name === detail.categoryName);
    if (matched) setCategoryId(matched.id);
  }, [categories, categoryId, detail.categoryName]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      await updateEvent({
        eventId,
        payload: {
          title,
          categoryId,
          hostName: hostName || null,
          venueName: venueName || null,
          address: address || null,
          detailAddress: detailAddress || null,
          kakaoPlaceId: detail.kakaoPlaceId,
          legalDongCode: detail.legalDongCode,
          latitude: detail.latitude,
          longitude: detail.longitude,
          startDate: startDate || null,
          endDate: endDate || null,
        },
      }).unwrap();
      onSaved("변경사항이 저장되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "저장에 실패했습니다."));
    }
  };

  return (
    <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
      <label className={styles.fieldFull}>
        <span>행사명</span>
        <input value={title} onChange={(event) => setTitle(event.target.value)} required />
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주최자명</span>
          <input value={hostName} onChange={(event) => setHostName(event.target.value)} />
        </label>
        <label className={styles.field}>
          <span>카테고리</span>
          <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)} required>
            <option value="" disabled>
              카테고리 선택
            </option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>시작일</span>
          <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        </label>
        <label className={styles.field}>
          <span>종료일</span>
          <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        </label>
      </div>

      <label className={styles.fieldFull}>
        <span>장소명</span>
        <input value={venueName} onChange={(event) => setVenueName(event.target.value)} />
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주소</span>
          <input value={address} onChange={(event) => setAddress(event.target.value)} />
        </label>
        <label className={styles.field}>
          <span>상세주소</span>
          <input value={detailAddress} onChange={(event) => setDetailAddress(event.target.value)} />
        </label>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      <div className={styles.formActions}>
        <button type="submit" className={styles.saveButton} disabled={isLoading}>
          {isLoading ? "저장 중..." : "저장"}
        </button>
      </div>
    </form>
  );
}