"use client";

import { useEffect, useRef, useState } from "react";
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight } from "lucide-react";
import styles from "./DateTimePicker.module.css";

interface DateTimePickerProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  onOpenChange?: (open: boolean) => void;
}

interface DayParts {
  year: number;
  month: number;
  day: number;
}

function formatDisplay(value: string): string {
  if (!value) return "";
  const [datePart, timePart] = value.split("T");
  return `${datePart.replaceAll("-", ".")} ${timePart ?? ""}`.trim();
}

function toDateParts(value: string): DayParts | null {
  if (!value) return null;
  const [datePart] = value.split("T");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) {
    return null;
  }

  const [year, month, day] = datePart.split("-").map(Number);
  const date = new Date(year, month - 1, day);

  if (
      date.getFullYear() !== year ||
      date.getMonth() !== month - 1 ||
      date.getDate() !== day
  ) {
    return null;
  }

  return { year, month, day };
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function toTimePart(value: string): string {
  const [, timePart] = value.split("T");
  if (!timePart) return "00:00";
  const [hour, minute] = timePart.split(":").map(Number);
  const snappedMinute = Math.round(minute / 5) * 5;
  const overflowHour = snappedMinute === 60;
  return `${pad(overflowHour ? (hour + 1) % 24 : hour)}:${pad(overflowHour ? 0 : snappedMinute)}`;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];
const HOURS = Array.from({ length: 24 }, (_, i) => pad(i));
const MINUTES = Array.from({ length: 12 }, (_, i) => pad(i * 5));

export function DateTimePicker({ value, onChange, placeholder = "날짜/시간 선택", onOpenChange }: DateTimePickerProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const selected = toDateParts(value);
  const today = new Date();
  const [viewYear, setViewYear] = useState(selected?.year ?? today.getFullYear());
  const [viewMonth, setViewMonth] = useState(selected ? selected.month - 1 : today.getMonth());
  const [pendingDay, setPendingDay] = useState<DayParts | null>(selected);
  const [pendingTime, setPendingTime] = useState(toTimePart(value));

  useEffect(() => {
    onOpenChange?.(open);
  }, [open, onOpenChange]);

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) setOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const openPicker = () => {
    const willOpen = !open;
    if (selected) {
      setViewYear(selected.year);
      setViewMonth(selected.month - 1);
    }
    setPendingDay(selected);
    setPendingTime(toTimePart(value));
    setOpen(willOpen);
    if (willOpen) {
      window.scrollBy({ top: 200, behavior: "smooth" });
    }
  };

  const firstDayOfMonth = new Date(viewYear, viewMonth, 1).getDay();
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const cells: (number | null)[] = [
    ...Array.from({ length: firstDayOfMonth }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  const selectDay = (day: number) => {
    setPendingDay({ year: viewYear, month: viewMonth + 1, day });
  };

  const changeMonth = (delta: number) => {
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
  };

  const handleApply = () => {
    if (!pendingDay) return;
    onChange(`${pendingDay.year}-${pad(pendingDay.month)}-${pad(pendingDay.day)}T${pendingTime}`);
    setOpen(false);
  };

  const [pendingHour, pendingMinute] = pendingTime.split(":");

  const changeHour = (hour: string) => setPendingTime(`${hour}:${pendingMinute}`);
  const changeMinute = (minute: string) => setPendingTime(`${pendingHour}:${minute}`);

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
                    !!pendingDay &&
                    pendingDay.year === viewYear &&
                    pendingDay.month === viewMonth + 1 &&
                    pendingDay.day === day
                  }
                  onClick={() => selectDay(day)}
                >
                  {day}
                </button>
              ),
            )}
          </div>
          <div className={styles.timeRow}>
            <span className={styles.timeLabel}>시간</span>
            <div className={styles.timeSelects}>
              <select
                className={styles.timeSelect}
                value={pendingHour}
                onChange={(event) => changeHour(event.target.value)}
                aria-label="시"
              >
                {HOURS.map((hour) => (
                  <option key={hour} value={hour}>
                    {hour}
                  </option>
                ))}
              </select>
              <span className={styles.timeColon}>:</span>
              <select
                className={styles.timeSelect}
                value={pendingMinute}
                onChange={(event) => changeMinute(event.target.value)}
                aria-label="분"
              >
                {MINUTES.map((minute) => (
                  <option key={minute} value={minute}>
                    {minute}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <button type="button" className={styles.applyButton} onClick={handleApply} disabled={!pendingDay || !pendingTime}>
            적용
          </button>
        </div>
      )}
    </div>
  );
}