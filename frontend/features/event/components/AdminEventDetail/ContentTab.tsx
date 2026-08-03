"use client";

import { useState } from "react";
import { ContentSectionsEditor } from "../ContentSectionsEditor/ContentSectionsEditor";
import { useUpsertAdminEventContentsMutation } from "../../api/adminEventDetailApi";
import type { AdminEventDetail, EventContentType } from "../../types/adminEventDetail";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminEventDetail.module.css";

interface ContentTabProps {
  eventId: string;
  detail: AdminEventDetail;
  onSaved: (message: string) => void;
}

export function ContentTab({ eventId, detail, onSaved }: ContentTabProps) {
  const [upsertContents, { isLoading }] = useUpsertAdminEventContentsMutation();
  const [sections, setSections] = useState<Partial<Record<EventContentType, string>>>(() => {
    const initial: Partial<Record<EventContentType, string>> = {};
    detail.contents.forEach((content) => {
      initial[content.contentType] = content.content;
    });
    return initial;
  });
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    setError(null);
    try {
      await upsertContents({ eventId, contents: sections }).unwrap();
      onSaved("콘텐츠가 저장되었습니다.");
    } catch (reason) {
      setError(queryErrorMessage(reason, "콘텐츠 저장에 실패했습니다."));
    }
  };

  return (
    <div className={styles.form}>
      <ContentSectionsEditor sections={sections} onChange={setSections} />
      {error && <p className={styles.error}>{error}</p>}
      <div className={styles.formActions}>
        <button type="button" className={styles.saveButton} disabled={isLoading} onClick={() => void handleSave()}>
          {isLoading ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}