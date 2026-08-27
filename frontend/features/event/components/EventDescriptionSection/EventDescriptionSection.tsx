import type { EventContentItem } from "../../types/event";
import styles from "./EventDescriptionSection.module.css";

interface EventDescriptionSectionProps {
  contents: EventContentItem[];
}

export function EventDescriptionSection({ contents }: EventDescriptionSectionProps) {
  const description = contents.find((content) => content.contentType === "DESCRIPTION");
  if (!description || !description.content.trim()) {
    return null;
  }

  return (
    <section className={styles.section}>
      <h2 className={styles.title}>행사 소개</h2>
      <p className={styles.body}>{description.content}</p>
    </section>
  );
}