"use client";

import { useEffect } from "react";
import { CheckCircle2 } from "lucide-react";
import styles from "./Toast.module.css";

interface ToastProps {
  message: string;
  onDismiss: () => void;
}

export function Toast({ message, onDismiss }: ToastProps) {
  useEffect(() => {
    const timer = window.setTimeout(onDismiss, 2500);
    return () => window.clearTimeout(timer);
  }, [onDismiss]);

  return (
    <div className={styles.toast} role="status">
      <CheckCircle2 size={18} className={styles.icon} />
      <span>{message}</span>
    </div>
  );
}