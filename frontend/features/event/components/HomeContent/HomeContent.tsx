"use client";

import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { CategoryQuickLinks } from "../CategoryQuickLinks/CategoryQuickLinks";
import { HomeEventSection } from "../HomeEventSection/HomeEventSection";
import styles from "./HomeContent.module.css";
import {HomeInfoStrip} from "@/features/event/components/HomeInfoStrip/HomeInfoStrip";

export function HomeContent() {
  const { data: categories = [] } = useGetEventCategoriesQuery();

  return (
    <div className={styles.page}>
      {/* TODO(marketing): 마케팅 배너 자리. imageUrl/linkUrl props만 받는 배너 컴포넌트로 교체 예정 */}
      <div className={styles.bannerSlot} />

      <CategoryQuickLinks categories={categories} />

      <HomeEventSection />

      <HomeInfoStrip />
    </div>
  );
}