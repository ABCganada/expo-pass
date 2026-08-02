"use client";

import { useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { BasicInfoEditor } from "./BasicInfoEditor";
import type { AdminEventDetail } from "../../types/adminEventDetail";
import styles from "./AdminEventDetail.module.css";

interface BasicInfoTabProps {
  eventId: string;
  detail: AdminEventDetail;
  onSaved: (message: string) => void;
}

export function BasicInfoTab({ eventId, detail, onSaved }: BasicInfoTabProps) {
  const { data: categories = [], isLoading: isLoadingCategories } = useGetEventCategoriesQuery();
  const [isEditing, setIsEditing] = useState(false);

  if (isLoadingCategories) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const categoryId = categories.find((category) => category.name === detail.categoryName)?.id ?? "";

  return (
    <BasicInfoEditor
      eventId={eventId}
      detail={detail}
      initialCategoryId={categoryId}
      isEditing={isEditing}
      onStartEdit={() => setIsEditing(true)}
      onCancel={() => setIsEditing(false)}
      onSaved={(message) => {
        setIsEditing(false);
        onSaved(message);
      }}
    />
  );
}