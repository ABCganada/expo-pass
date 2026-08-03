"use client";

import { useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { BasicInfoEditor } from "./BasicInfoEditor";
import { ManagerReassignPanel } from "./ManagerReassignPanel";
import type { EventManagementDetail } from "../../types/eventManagementDetail";
import type { EventRole } from "../../types/eventRole";
import styles from "./EventDetail.module.css";

interface BasicInfoTabProps {
  eventId: string;
  detail: EventManagementDetail;
  mode: EventRole;
  onSaved: (message: string) => void;
}

export function BasicInfoTab({ eventId, detail, mode, onSaved }: BasicInfoTabProps) {
  const { data: categories = [], isLoading: isLoadingCategories } = useGetEventCategoriesQuery();
  const [isEditing, setIsEditing] = useState(false);

  if (isLoadingCategories) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const categoryId = categories.find((category) => category.name === detail.categoryName)?.id ?? "";

  return (
    <>
      <BasicInfoEditor
        eventId={eventId}
        detail={detail}
        initialCategoryId={categoryId}
        isEditing={isEditing}
        mode={mode}
        onStartEdit={() => setIsEditing(true)}
        onCancel={() => setIsEditing(false)}
        onSaved={(message) => {
          setIsEditing(false);
          onSaved(message);
        }}
      />
      {mode === "admin" && <ManagerReassignPanel eventId={eventId} detail={detail} onSaved={onSaved} />}
    </>
  );
}