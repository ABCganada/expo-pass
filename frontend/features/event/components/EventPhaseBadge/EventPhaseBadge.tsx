import { computeDdayLabel, computeEventPhase } from "../../utils/eventPhase";
import styles from "./EventPhaseBadge.module.css";

interface EventPhaseBadgeProps {
  startDate: string;
  endDate: string;
  className?: string;
}

export function EventPhaseBadge({ startDate, endDate, className }: EventPhaseBadgeProps) {
  const phase = computeEventPhase(startDate, endDate);
  const label = computeDdayLabel(phase, startDate);

  return (
    <span className={className ? `${styles.badge} ${className}` : styles.badge} data-phase={phase}>
      {label}
    </span>
  );
}