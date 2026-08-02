"use client";

import { useEffect, useRef, useState } from "react";
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight } from "lucide-react";
import styles from "./DatePicker.module.css";

interface DatePickerProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

function formatDisplay(value: string): string {
  return value ? value.replaceAll("-", ".") : "";
}

function toDateParts(value: string): { year: number; month: number; day: number } | null {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  return { year, month, day };
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

export function DatePicker({ value, onChange, placeholder = "날짜 선택" }: DatePickerProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const selected = toDateParts(value);
  const today = new Date();
  const [viewYear, setViewYear] = useState(selected?.year ?? today.getFullYear());
  const [viewMonth, setViewMonth] = useState(selected ? selected.month - 1 : today.getMonth());

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) setOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const openPicker = () => {
    if (selected) {
      setViewYear(selected.year);
      setViewMonth(selected.month - 1);
    }
    setOpen((prev) => !prev);
  };

  const firstDayOfMonth = new Date(viewYear, viewMonth, 1).getDay();
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const cells: (number | null)[] = [
    ...Array.from({ length: firstDayOfMonth }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  const selectDay = (day: number) => {
    onChange(`${viewYear}-${pad(viewMonth + 1)}-${pad(day)}`);
    setOpen(false);
  };

  const changeMonth = (delta: number) => {
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
  };

  return (
    <div className={styles.wrapper} ref={containerRef}>
      <button type="button" className={styles.trigger} onClick={openPicker}>
        <span className={value ? styles.value : styles.placeholder}>
          {value ? formatDisplay(value) : placeholder}
        </span>
        <CalendarIcon size={16} className={styles.icon} />
      </button>

      {open && (
        <div className={styles.popup} role="dialog">
          <div className={styles.header}>
            <button type="button" className={styles.navButton} onClick={() => changeMonth(-1)} aria-label="이전 달">
              <ChevronLeft size={16} />
            </button>
            <span className={styles.monthLabel}>
              {viewYear}년 {viewMonth + 1}월
            </span>
            <button type="button" className={styles.navButton} onClick={() => changeMonth(1)} aria-label="다음 달">
              <ChevronRight size={16} />
            </button>
          </div>
          <div className={styles.weekdays}>
            {WEEKDAYS.map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>
          <div className={styles.grid}>
            {cells.map((day, index) =>
              day === null ? (
                <span key={`empty-${index}`} />
              ) : (
                <button
                  key={day}
                  type="button"
                  className={styles.day}
                  data-selected={
                    !!selected &&
                    selected.year === viewYear &&
                    selected.month === viewMonth + 1 &&
                    selected.day === day
                  }
                  onClick={() => selectDay(day)}
                >
                  {day}
                </button>
              ),
            )}
          </div>
        </div>
      )}
    </div>
  );
}