"use client";

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

  if (isLoadingCategories) {
    return <div className={styles.state}>불러오는 중...</div>;
  }

  const categoryId = categories.find((category) => category.name === detail.categoryName)?.id ?? "";

  return <BasicInfoEditor eventId={eventId} detail={detail} initialCategoryId={categoryId} onSaved={onSaved} />;
}