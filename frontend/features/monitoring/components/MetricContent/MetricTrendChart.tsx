import { Clock3 } from "lucide-react";
import type { MetricTrend } from "../../types/metric";
import styles from "./MetricContent.module.css";

const WIDTH = 640;
const HEIGHT = 220;
const PADDING = { top: 18, right: 18, bottom: 34, left: 18 };

interface NumericPoint {
  timestamp: number;
  value: number;
}

function formatValue(value: number, unit: string): string {
  const maximumFractionDigits = unit === "records" ? 0 : unit === "req/s" ? 2 : 1;
  return `${new Intl.NumberFormat("ko-KR", { maximumFractionDigits }).format(value)} ${unit}`;
}

function formatTime(timestamp: number): string {
  return new Date(timestamp * 1000).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function MetricTrendChart({ trend }: { trend: MetricTrend }) {
  const points: NumericPoint[] = trend.points
    .map((point) => ({ timestamp: point.timestamp, value: Number(point.value) }))
    .filter((point) => Number.isFinite(point.value));

  if (points.length === 0) {
    return (
      <article className={styles.chartCard}>
        <div className={styles.chartHeading}>
          <div>
            <span className={styles.chartEyebrow}>24시간 추이</span>
            <h2>{trend.title}</h2>
          </div>
        </div>
        <div className={styles.chartEmpty}>표시할 데이터가 없습니다.</div>
      </article>
    );
  }

  const values = points.map((point) => point.value);
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const range = rawMax - rawMin || Math.max(Math.abs(rawMax) * 0.1, 1);
  const min = rawMin - range * 0.08;
  const max = rawMax + range * 0.08;
  const firstTimestamp = points[0].timestamp;
  const lastTimestamp = points.at(-1)?.timestamp ?? firstTimestamp;
  const timeRange = lastTimestamp - firstTimestamp || 1;
  const innerWidth = WIDTH - PADDING.left - PADDING.right;
  const innerHeight = HEIGHT - PADDING.top - PADDING.bottom;
  const coordinates = points.map((point) => ({
    x: PADDING.left + ((point.timestamp - firstTimestamp) / timeRange) * innerWidth,
    y: PADDING.top + (1 - (point.value - min) / (max - min)) * innerHeight,
  }));
  const linePath = coordinates
    .map((point, index) => `${index === 0 ? "M" : "L"}${point.x.toFixed(2)},${point.y.toFixed(2)}`)
    .join(" ");
  const areaPath = `${linePath} L${coordinates.at(-1)?.x.toFixed(2)},${HEIGHT - PADDING.bottom} `
    + `L${coordinates[0].x.toFixed(2)},${HEIGHT - PADDING.bottom} Z`;
  const latest = points.at(-1)?.value ?? 0;
  const gradientId = `metric-gradient-${trend.id}`;

  return (
    <article className={styles.chartCard}>
      <div className={styles.chartHeading}>
        <div>
          <span className={styles.chartEyebrow}>24시간 추이</span>
          <h2>{trend.title}</h2>
        </div>
        <strong className={styles.chartLatest}>{formatValue(latest, trend.unit)}</strong>
      </div>

      <div className={styles.chartCanvas}>
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${trend.title} 24시간 시계열 그래프`}>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="currentColor" stopOpacity="0.22" />
              <stop offset="100%" stopColor="currentColor" stopOpacity="0" />
            </linearGradient>
          </defs>
          {[0, 1, 2, 3].map((line) => {
            const y = PADDING.top + (innerHeight / 3) * line;
            return <line key={line} x1={PADDING.left} y1={y} x2={WIDTH - PADDING.right} y2={y} className={styles.gridLine} />;
          })}
          <path d={areaPath} fill={`url(#${gradientId})`} className={styles.areaPath} />
          <path d={linePath} className={styles.linePath} />
          <circle
            cx={coordinates.at(-1)?.x}
            cy={coordinates.at(-1)?.y}
            r="4"
            className={styles.latestPoint}
          />
          <text x={PADDING.left} y={HEIGHT - 10} className={styles.axisLabel}>{formatTime(firstTimestamp)}</text>
          <text x={WIDTH / 2} y={HEIGHT - 10} textAnchor="middle" className={styles.axisLabel}>
            {formatTime(firstTimestamp + timeRange / 2)}
          </text>
          <text x={WIDTH - PADDING.right} y={HEIGHT - 10} textAnchor="end" className={styles.axisLabel}>
            {formatTime(lastTimestamp)}
          </text>
        </svg>
      </div>

      <div className={styles.chartFooter}>
        <span><Clock3 aria-hidden="true" /> 1분 간격</span>
        <span>최저 {formatValue(rawMin, trend.unit)}</span>
        <span>최고 {formatValue(rawMax, trend.unit)}</span>
      </div>
    </article>
  );
}
