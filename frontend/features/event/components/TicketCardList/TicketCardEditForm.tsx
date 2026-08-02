import { useState } from "react";
import { DateTimePicker } from "../DateTimePicker/DateTimePicker";
import type { TicketDraft } from "./ticketUtils";
import styles from "./TicketCard.module.css";

interface TicketCardEditFormProps {
  draft: TicketDraft;
  onChange: (patch: Partial<TicketDraft>) => void;
  isNew: boolean;
  originalSaleStartAt: string | null;
  isSaving: boolean;
  onSave: () => void;
  onCancel: () => void;
}

export function TicketCardEditForm({
  draft,
  onChange,
  isNew,
  originalSaleStartAt,
  isSaving,
  onSave,
  onCancel,
}: TicketCardEditFormProps) {
  const [startPickerOpen, setStartPickerOpen] = useState(false);
  const [endPickerOpen, setEndPickerOpen] = useState(false);
  const isAnyPickerOpen = startPickerOpen || endPickerOpen;
  const isQuantityLocked = !isNew && !!originalSaleStartAt && new Date(originalSaleStartAt) <= new Date();

  return (
    <div className={styles.card}>
      <div className={styles.row}>
        <span className={styles.label}>티켓명</span>
        <input
          className={styles.titleInput}
          placeholder="티켓명"
          value={draft.name}
          onChange={(event) => onChange({ name: event.target.value })}
        />
      </div>
      <div className={styles.row}>
        <span className={styles.label}>가격</span>
        <input
          type="number"
          className={styles.priceInput}
          placeholder="가격"
          value={draft.price}
          onChange={(event) => onChange({ price: event.target.value })}
        />
      </div>
      <div className={styles.divider} />
      <div className={styles.row}>
        <span className={styles.label}>총수량</span>
        {isQuantityLocked ? (
          <span className={styles.readonlyValue} title="판매 시작 이후에는 총 수량을 수정할 수 없습니다">
            {draft.quantityTotal}
          </span>
        ) : (
          <input
            type="number"
            className={styles.valueInput}
            placeholder="수량"
            value={draft.quantityTotal}
            onChange={(event) => onChange({ quantityTotal: event.target.value })}
          />
        )}
      </div>
      <div className={styles.row}>
        <span className={styles.label}>인당 최대</span>
        <input
          type="number"
          className={styles.valueInput}
          placeholder="인당 최대"
          value={draft.maxPurchasePerUser}
          onChange={(event) => onChange({ maxPurchasePerUser: event.target.value })}
        />
      </div>
      <div className={styles.row}>
        <span className={styles.label}>판매기간</span>
        <div className={styles.dateRange}>
          <DateTimePicker
            value={draft.saleStartAt}
            onChange={(saleStartAt) => onChange({ saleStartAt })}
            onOpenChange={setStartPickerOpen}
            placeholder="시작 일시"
          />
          <span>~</span>
          <DateTimePicker
            value={draft.saleEndAt}
            onChange={(saleEndAt) => onChange({ saleEndAt })}
            onOpenChange={setEndPickerOpen}
            placeholder="종료 일시"
          />
        </div>
      </div>
      {isAnyPickerOpen && <div className={styles.pickerSpacer} />}
      <div className={styles.cardActions}>
        <button type="button" className={styles.cancelButton} onClick={onCancel}>
          취소
        </button>
        <button type="button" className={styles.saveButton} disabled={isSaving} onClick={onSave}>
          저장
        </button>
      </div>
    </div>
  );
}