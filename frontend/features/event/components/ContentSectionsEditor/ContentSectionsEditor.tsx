"use client";

import { Plus, Trash2 } from "lucide-react";
import type { EventContentType } from "../../types/adminEventDetail";
import styles from "./ContentSectionsEditor.module.css";

const SECTION_LABELS: Record<EventContentType, string> = {
  DESCRIPTION: "설명",
  NOTICE: "공지",
  LOCATION_GUIDE: "오시는 길",
};

const SECTION_ORDER: EventContentType[] = ["DESCRIPTION", "NOTICE", "LOCATION_GUIDE"];
const BASIC_SECTION_TYPE: EventContentType = "DESCRIPTION";

interface ContentSectionsEditorProps {
  sections: Partial<Record<EventContentType, string>>;
  onChange: (sections: Partial<Record<EventContentType, string>>) => void;
}

export function ContentSectionsEditor({ sections, onChange }: ContentSectionsEditorProps) {
  const activeTypes = SECTION_ORDER.filter((type) => sections[type] !== undefined);
  const availableTypes = SECTION_ORDER.filter((type) => sections[type] === undefined);

  const addSection = (type: EventContentType) => {
    onChange({ ...sections, [type]: "" });
  };

  const removeSection = (type: EventContentType) => {
    const next = { ...sections };
    delete next[type];
    onChange(next);
  };

  const updateContent = (type: EventContentType, content: string) => {
    onChange({ ...sections, [type]: content });
  };

  return (
    <div className={styles.editor}>
      {availableTypes.length > 0 && (
        <div className={styles.toolbar}>
          {availableTypes.map((type) => (
            <button key={type} type="button" className={styles.addButton} onClick={() => addSection(type)}>
              <Plus size={14} />
              {SECTION_LABELS[type]} 섹션 추가
            </button>
          ))}
        </div>
      )}

      {activeTypes.length === 0 ? (
        <p className={styles.empty}>추가된 섹션이 없습니다.</p>
      ) : (
        <div className={styles.sections}>
          {activeTypes.map((type) => {
            const isBasic = type === BASIC_SECTION_TYPE;
            return (
              <div key={type} className={styles.section} data-basic={isBasic}>
                <div className={styles.sectionHeader}>
                  <span className={styles.sectionTitle}>
                    {SECTION_LABELS[type]}
                    <span className={styles.sectionBadge} data-basic={isBasic}>
                      {isBasic ? "기본" : "선택"}
                    </span>
                  </span>
                  <button type="button" className={styles.deleteButton} onClick={() => removeSection(type)}>
                    <Trash2 size={14} />
                    삭제
                  </button>
                </div>
                <textarea
                  className={styles.textarea}
                  value={sections[type] ?? ""}
                  onChange={(event) => updateContent(type, event.target.value)}
                  rows={5}
                  placeholder={`${SECTION_LABELS[type]} 내용을 입력하세요.`}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}