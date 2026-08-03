import Link from "next/link";
import {
  Building2,
  Cpu,
  GraduationCap,
  LayoutGrid,
  Palette,
  Plane,
  Sparkles,
  Tag,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";
import type { EventCategory } from "../../types/event";
import styles from "./CategoryQuickLinks.module.css";

const ICON_BY_NAME: Record<string, LucideIcon> = {
  "IT/테크": Cpu,
  "건축/인테리어": Building2,
  "식음료/푸드": UtensilsCrossed,
  "교육/취업": GraduationCap,
  "뷰티/헬스": Sparkles,
  "문화/예술": Palette,
  "여행/레저": Plane,
};

interface CategoryQuickLinksProps {
  categories: EventCategory[];
}

export function CategoryQuickLinks({ categories }: CategoryQuickLinksProps) {
  const activeCategories = categories.filter((category) => category.active);

  return (
    <nav className={styles.wrap} aria-label="박람회 카테고리 바로가기">
      <Link href="/exhibitions" className={styles.tile}>
        <span className={styles.iconWrap}>
          <LayoutGrid size={22} aria-hidden />
        </span>
        <span className={styles.label}>전체</span>
      </Link>

      {activeCategories.map((category) => {
        const Icon = ICON_BY_NAME[category.name] ?? Tag;
        return (
          <Link
            key={category.id}
            href={`/exhibitions?category=${category.id}`}
            className={styles.tile}
          >
            <span className={styles.iconWrap}>
              <Icon size={22} aria-hidden />
            </span>
            <span className={styles.label}>{category.name}</span>
          </Link>
        );
      })}
    </nav>
  );
}