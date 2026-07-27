import type { LucideIcon } from "lucide-react";
import styles from "./FeaturePlaceholder.module.css";

interface FeaturePlaceholderProps {
  title: string;
  description: string;
  icon: LucideIcon;
}

export function FeaturePlaceholder({ title, description, icon: Icon }: FeaturePlaceholderProps) {
  return (
    <section className={styles.page}>
      <div className={styles.iconWrap}><Icon aria-hidden="true" /></div>
      <p className={styles.eyebrow}>LAST MISSION</p>
      <h1>{title}</h1>
      <p className={styles.description}>{description}</p>
      <span className={styles.badge}>API 연동 준비 중</span>
    </section>
  );
}
