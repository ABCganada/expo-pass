"use client";

import { useState } from "react";
import { useUpdateManagerEventMutation } from "../../api/managerEventDetailApi";
import { useChangeAdminEventManagerMutation } from "../../api/adminEventApi";
import { BasicInfoForm, type BasicInfoFormValues } from "../BasicInfoForm/BasicInfoForm";
import type { EventManagementDetail } from "../../types/eventManagementDetail";
import type { EventRole } from "../../types/eventRole";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./EventDetail.module.css";

interface BasicInfoEditorProps {
  eventId: string;
  detail: EventManagementDetail;
  initialCategoryId: string;
  isEditing: boolean;
  mode: EventRole;
  onStartEdit: () => void;
  onCancel: () => void;
  onSaved: (message: string) => void;
}

function toFormValues(detail: EventManagementDetail, categoryId: string): BasicInfoFormValues {
  return {
    title: detail.title,
    hostName: detail.hostName ?? "",
    categoryId,
    manager: detail.managerId ? { id: detail.managerId, name: detail.managerName ?? "", email: "" } : null,
    startDate: detail.startDate ?? "",
    endDate: detail.endDate ?? "",
    venueName: detail.venueName ?? "",
    address: detail.address ?? "",
    detailAddress: detail.detailAddress ?? "",
    latitude: detail.latitude,
    longitude: detail.longitude,
    kakaoPlaceId: detail.kakaoPlaceId,
    legalDongCode: detail.legalDongCode,
  };
}

/** 저장 mutation과 폼 상태를 담당한다. 필드 렌더링은 BasicInfoForm에 위임한다. */
export function BasicInfoEditor({
  eventId,
  detail,
  initialCategoryId,
  isEditing,
  mode,
  onStartEdit,
  onCancel,
  onSaved,
}: BasicInfoEditorProps) {
  const canEdit = mode === "manager";
  const [updateEvent, { isLoading: isUpdating }] = useUpdateManagerEventMutation();
  const [changeManager, { isLoading: isChangingManager }] = useChangeAdminEventManagerMutation();
  const [values, setValues] = useState<BasicInfoFormValues>(() => toFormValues(detail, initialCategoryId));
  const [error, setError] = useState<string | null>(null);

  // detail/initialCategoryId가 바뀌면(예: 저장 후 재조회) 편집 중이던 값도 최신 서버 상태로 다시 맞춘다.
  const [syncedDetail, setSyncedDetail] = useState(detail);
  const [syncedCategoryId, setSyncedCategoryId] = useState(initialCategoryId);
  if (detail !== syncedDetail || initialCategoryId !== syncedCategoryId) {
    setSyncedDetail(detail);
    setSyncedCategoryId(initialCategoryId);
    setValues(toFormValues(detail, initialCategoryId));
  }

  const handleChange = (patch: Partial<BasicInfoFormValues>) => setValues((prev) => ({ ...prev, ...patch }));

  const handleCancel = () => {
    setError(null);
    setValues(toFormValues(detail, initialCategoryId));
    onCancel();
  };

  const handleSubmit = async () => {
    setError(null);
    try {
      await updateEvent({
        eventId,
        payload: {
          title: values.title,
          categoryId: values.categoryId,
          hostName: values.hostName || null,
          venueName: values.venueName || null,
          address: values.address || null,
          detailAddress: values.detailAddress || null,
          kakaoPlaceId: values.kakaoPlaceId,
          legalDongCode: values.legalDongCode,
          latitude: values.latitude,
          longitude: values.longitude,
          startDate: values.startDate || null,
          endDate: values.endDate || null,
        },
      }).unwrap();
      if (values.manager && values.manager.id !== detail.managerId) {
        await changeManager({ eventId, managerId: values.manager.id }).unwrap();
      }
      onSaved("변경사항이 저장되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "저장에 실패했습니다."));
    }
  };

  const isSubmitting = isUpdating || isChangingManager;

  return (
    <BasicInfoForm
      values={values}
      onChange={handleChange}
      onValidSubmit={() => void handleSubmit()}
      readOnly={!isEditing}
      footer={
        isEditing ? (
          <>
            {error && <p className={styles.error}>{error}</p>}
            <div className={styles.formActions}>
              <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isSubmitting}>
                취소
              </button>
              <button type="submit" className={styles.saveButton} disabled={isSubmitting}>
                {isSubmitting ? "저장 중..." : "저장"}
              </button>
            </div>
          </>
        ) : canEdit ? (
          <div className={styles.viewHeader}>
            <button type="button" className={styles.editButton} onClick={onStartEdit}>
              수정
            </button>
          </div>
        ) : null
      }
    />
  );
}