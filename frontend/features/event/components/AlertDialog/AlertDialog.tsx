"use client";

import styles from "./AlertDialog.module.css";

interface AlertDialogProps {
  title: string;
  description?: string;
  confirmLabel?: string;
  onConfirm: () => void;
}

export function AlertDialog({ title, description, confirmLabel = "확인", onConfirm }: AlertDialogProps) {
  return (
    <div className={styles.backdrop} role="presentation" onMouseDown={onConfirm}>
      <section
        className={styles.dialog}
        role="alertdialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 className={styles.title}>{title}</h2>
        {description && <p className={styles.description}>{description}</p>}
        <div className={styles.actions}>
          <button type="button" className={styles.confirm} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}