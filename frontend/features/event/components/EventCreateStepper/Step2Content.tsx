"use client";

import { useState } from "react";
import { ContentSectionsEditor } from "../ContentSectionsEditor/ContentSectionsEditor";
import { useUpsertManagerEventContentsMutation } from "../../api/managerEventDetailApi";
import type { EventContentItem, EventContentType } from "../../types/eventManagementDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import formStyles from "../EventDetail/EventDetail.module.css";
import styles from "./EventCreateStepper.module.css";

interface Step2ContentProps {
  eventId: string;
  initialContents: EventContentItem[];
  onBack: () => void;
  onNext: () => void;
}

export function Step2Content({ eventId, initialContents, onBack, onNext }: Step2ContentProps) {
  const [upsertContents, { isLoading }] = useUpsertManagerEventContentsMutation();
  const [sections, setSections] = useState<Partial<Record<EventContentType, string>>>(() => {
    const initial: Partial<Record<EventContentType, string>> = {};
    initialContents.forEach((content) => {
      initial[content.contentType] = content.content;
    });
    return initial;
  });
  const [error, setError] = useState<string | null>(null);

  const handleNext = async () => {
    setError(null);
    if (Object.keys(sections).length === 0) {
      onNext();
      return;
    }
    try {
      await upsertContents({ eventId, contents: sections }).unwrap();
      onNext();
    } catch (reason) {
      setError(queryErrorMessage(reason, "콘텐츠 저장에 실패했습니다."));
    }
  };

  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <ContentSectionsEditor sections={sections} onChange={setSections} />
        {error && <p className={formStyles.error}>{error}</p>}
        <div className={styles.stepActions}>
          <button type="button" className={styles.backButton} onClick={onBack}>
            이전
          </button>
          <button type="button" className={styles.nextButton} disabled={isLoading} onClick={() => void handleNext()}>
            {isLoading ? "저장 중..." : "다음"}
          </button>
        </div>
      </div>
    </div>
  );
}