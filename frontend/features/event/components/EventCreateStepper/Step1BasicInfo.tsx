"use client";

import { BasicInfoForm, type BasicInfoFormValues } from "../BasicInfoForm/BasicInfoForm";
import { useAuth } from "@/features/auth/hooks/useAuth";
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

export function Step1BasicInfo({ values, onChange, onNext, isSubmitting, submitError }: Step1BasicInfoProps) {
  const { user } = useAuth();
  const isAdmin = user?.roles.includes("ADMIN") ?? false;

  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <BasicInfoForm
          values={values}
          onChange={onChange}
          onValidSubmit={onNext}
          canEditManager={isAdmin}
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