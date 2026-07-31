"use client";

import type { EventCategory } from "../../types/event";
import styles from "./CategoryTabs.module.css";

interface CategoryTabsProps {
  categories: EventCategory[];
  activeCategoryId: string | null;
  onSelect: (categoryId: string | null) => void;
}

export function CategoryTabs({ categories, activeCategoryId, onSelect }: CategoryTabsProps) {
  const activeCategories = categories.filter((category) => category.active);

  return (
    <div className={styles.wrap} role="tablist" aria-label="박람회 카테고리">
      <button
        type="button"
        role="tab"
        aria-selected={activeCategoryId === null}
        className={styles.tab}
        data-active={activeCategoryId === null}
        onClick={() => onSelect(null)}
      >
        전체
      </button>
      {activeCategories.map((category) => (
        <button
          key={category.id}
          type="button"
          role="tab"
          aria-selected={activeCategoryId === category.id}
          className={styles.tab}
          data-active={activeCategoryId === category.id}
          onClick={() => onSelect(category.id)}
        >
          {category.name}
        </button>
      ))}
    </div>
  );
}