"use client";

import { BasicInfoForm, type BasicInfoFormValues } from "../BasicInfoForm/BasicInfoForm";
import formStyles from "../EventDetail/EventDetail.module.css";
import styles from "./EventCreateStepper.module.css";

export type Step1Values = BasicInfoFormValues;

interface Step1BasicInfoProps {
  values: Step1Values;
  onChange: (patch: Partial<Step1Values>) => void;
  onNext: () => void;
  isSubmitting: boolean;
  submitError: string | null;
}

/** 담당자는 항상 작성자 본인으로 self-assign되므로 읽기 전용으로만 표시한다. */
export function Step1BasicInfo({ values, onChange, onNext, isSubmitting, submitError }: Step1BasicInfoProps) {
  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <BasicInfoForm
          values={values}
          onChange={onChange}
          onValidSubmit={onNext}
          footer={
            <>
              {submitError && <p className={formStyles.error}>{submitError}</p>}
              <div className={styles.stepFooter}>
                <div className={styles.stepActionsSingle}>
                  <button type="submit" className={styles.nextButton} disabled={isSubmitting}>
                    {isSubmitting ? "저장 중..." : "다음"}
                  </button>
                </div>
              </div>
            </>
          }
        />
      </div>
    </div>
  );
}