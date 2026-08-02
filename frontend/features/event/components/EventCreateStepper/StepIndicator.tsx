"use client";

import { Check } from "lucide-react";
import styles from "./EventCreateStepper.module.css";

const STEPS = [
  { step: 1, label: "기본정보" },
  { step: 2, label: "콘텐츠" },
  { step: 3, label: "이미지" },
  { step: 4, label: "티켓" },
];

interface StepIndicatorProps {
  currentStep: number;
  maxReachedStep: number;
  onStepClick: (step: number) => void;
}

export function StepIndicator({ currentStep, maxReachedStep, onStepClick }: StepIndicatorProps) {
  return (
    <div className={styles.indicator}>
      {STEPS.map((item, index) => {
        const isCompleted = item.step < currentStep;
        const isCurrent = item.step === currentStep;
        const isClickable = item.step <= maxReachedStep && item.step !== currentStep;
        return (
          <div key={item.step} className={styles.indicatorItem}>
            <button
              type="button"
              className={styles.node}
              data-completed={isCompleted}
              data-current={isCurrent}
              disabled={!isClickable}
              onClick={() => onStepClick(item.step)}
            >
              {isCompleted ? <Check size={14} /> : item.step}
            </button>
            <span className={styles.nodeLabel} data-current={isCurrent}>
              {item.label}
            </span>
            {index < STEPS.length - 1 && <span className={styles.connector} data-completed={isCompleted} />}
          </div>
        );
      })}
      <span className={styles.connector} />
      <div className={styles.indicatorItem}>
        <span className={styles.doneNode}>{currentStep > STEPS.length ? <Check size={14} /> : ""}</span>
        <span className={styles.nodeLabel}>완료</span>
      </div>
    </div>
  );
}