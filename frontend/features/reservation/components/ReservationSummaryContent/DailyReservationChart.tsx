"use client";

import { useState } from "react";
import type { DailyReservationCount } from "../../types/admin";
import styles from "./DailyReservationChart.module.css";

interface DailyReservationChartProps {
  data: DailyReservationCount[];
}

function niceCeil(value: number): number {
  if (value <= 0) return 1;
  const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
  const normalized = value / magnitude;
  const niceNormalized = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return niceNormalized * magnitude;
}

function formatAxisDate(value: string): string {
  const date = new Date(value);
  return date.toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" });
}

function formatTooltipDate(value: string): string {
  const date = new Date(value);
  return date.toLocaleDateString("ko-KR", { month: "long", day: "numeric", weekday: "short" });
}

export function DailyReservationChart({ data }: DailyReservationChartProps) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  if (data.length === 0) {
    return (
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>예약 추이</h2>
        <div className={styles.empty}>표시할 예약 데이터가 없습니다</div>
      </div>
    );
  }

  const maxCount = Math.max(...data.map((d) => d.count));
  const axisMax = niceCeil(maxCount);
  // axisMax가 작을 때(예: 1) 중간값이 위/아래 눈금과 겹칠 수 있어 중복은 제거한다.
  const ticks = Array.from(new Set([axisMax, Math.round(axisMax / 2), 0])).sort((a, b) => b - a);

  return (
    <div className={styles.section}>
      <h2 className={styles.sectionTitle}>예약 추이</h2>
      <div className={styles.chart}>
        <div className={styles.yAxis}>
          {ticks.map((tick) => (
            <span key={tick} className={styles.yTick}>
              {tick.toLocaleString("ko-KR")}
            </span>
          ))}
        </div>
        <div className={styles.plot}>
          <div className={styles.gridLines} aria-hidden="true">
            {ticks.map((tick) => (
              <div key={tick} className={styles.gridLine} />
            ))}
          </div>
          <div className={styles.bars}>
            {data.map((d, i) => {
              const heightPct = axisMax === 0 ? 0 : (d.count / axisMax) * 100;
              return (
                <div
                  key={d.date}
                  className={styles.column}
                  onPointerEnter={() => setHoverIndex(i)}
                  onPointerLeave={() => setHoverIndex((prev) => (prev === i ? null : prev))}
                  onFocus={() => setHoverIndex(i)}
                  onBlur={() => setHoverIndex((prev) => (prev === i ? null : prev))}
                  tabIndex={0}
                >
                  {hoverIndex === i && (
                    <div className={styles.tooltip} role="tooltip">
                      <strong className={styles.tooltipValue}>{d.count.toLocaleString("ko-KR")}건</strong>
                      <span className={styles.tooltipLabel}>{formatTooltipDate(d.date)}</span>
                    </div>
                  )}
                  <div className={styles.barTrack}>
                    <div
                      className={styles.bar}
                      data-hovered={hoverIndex === i}
                      style={{ height: `${Math.max(heightPct, d.count > 0 ? 2 : 0)}%` }}
                    />
                  </div>
                  <span className={styles.xLabel}>{formatAxisDate(d.date)}</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
