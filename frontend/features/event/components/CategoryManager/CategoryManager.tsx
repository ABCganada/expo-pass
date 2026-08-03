"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { useGetAllAdminCategoriesQuery, useToggleCategoryActiveMutation } from "../../api/adminCategoryApi";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { Toast } from "../Toast/Toast";
import type { EventCategory } from "../../types/event";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./CategoryManager.module.css";

export function CategoryManager() {
  const router = useRouter();
  const { data: categories = [], isLoading, isError, error } = useGetAllAdminCategoriesQuery();
  const [toggleActive, { isLoading: isToggling }] = useToggleCategoryActiveMutation();
  const [pendingDeactivate, setPendingDeactivate] = useState<EventCategory | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const applyToggle = async (category: EventCategory) => {
    setActionError(null);
    try {
      await toggleActive(category.id).unwrap();
      setToast(category.active ? "카테고리를 비활성화했습니다." : "카테고리를 활성화했습니다.");
    } catch (reason) {
      setActionError(queryErrorMessage(reason, "카테고리 상태 변경에 실패했습니다."));
    }
  };

  const handleToggleClick = (category: EventCategory) => {
    if (category.active) {
      setPendingDeactivate(category);
      return;
    }
    void applyToggle(category);
  };

  const handleConfirmDeactivate = () => {
    if (!pendingDeactivate) return;
    const category = pendingDeactivate;
    setPendingDeactivate(null);
    void applyToggle(category);
  };

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <button type="button" className={styles.backButton} onClick={() => router.push("/admin/events")}>
          <ArrowLeft size={16} />
          목록으로
        </button>
        <h1 className={styles.title}>카테고리 관리</h1>
      </header>

      {actionError && <p className={styles.error}>{actionError}</p>}

      {isError ? (
        <div className={styles.state}>{queryErrorMessage(error, "카테고리 목록을 불러오지 못했습니다.")}</div>
      ) : isLoading ? (
        <div className={styles.state}>불러오는 중...</div>
      ) : categories.length === 0 ? (
        <div className={styles.state}>등록된 카테고리가 없습니다.</div>
      ) : (
        <div className={styles.card}>
          {categories.map((category) => (
            <div key={category.id} className={styles.row} data-active={category.active}>
              <span className={styles.name}>{category.name}</span>
              <label className={styles.switch}>
                <input
                  type="checkbox"
                  role="switch"
                  checked={category.active}
                  disabled={isToggling}
                  onChange={() => handleToggleClick(category)}
                  aria-label={`${category.name} 활성화 여부`}
                />
                <span className={styles.slider} />
              </label>
            </div>
          ))}
        </div>
      )}

      {pendingDeactivate && (
        <ConfirmDialog
          title="카테고리를 비활성화할까요?"
          description="이 카테고리를 사용 중인 행사가 있을 수 있습니다."
          confirmLabel="비활성화"
          danger
          onConfirm={handleConfirmDeactivate}
          onCancel={() => setPendingDeactivate(null)}
        />
      )}

      {toast && <Toast message={toast} onDismiss={() => setToast(null)} />}
    </section>
  );
}