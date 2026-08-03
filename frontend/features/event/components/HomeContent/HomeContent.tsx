"use client";

import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { BannerSlider } from "@/features/marketing/components/BannerSlider/BannerSlider";
import { CategoryQuickLinks } from "../CategoryQuickLinks/CategoryQuickLinks";
import { HomeEventSection } from "../HomeEventSection/HomeEventSection";
import { HomeInfoStrip } from "@/features/event/components/HomeInfoStrip/HomeInfoStrip";
import styles from "./HomeContent.module.css";

export function HomeContent() {
  const { data: categories = [] } = useGetEventCategoriesQuery();

  return (
    <div className={styles.page}>
      <div className={styles.bannerWrapper}>
        <BannerSlider />
      </div>

      <CategoryQuickLinks categories={categories} />

      <HomeEventSection />

      <HomeInfoStrip />
    </div>
  );
}