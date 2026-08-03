"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useCreateDraftEventMutation, useChangeEventManagerMutation } from "@/features/event/api/eventCreateApi";
import { useUpdateAdminEventMutation, useGetAdminEventDetailQuery } from "@/features/event/api/adminEventDetailApi";
import { StepIndicator } from "@/features/event/components/EventCreateStepper/StepIndicator";
import { Step1BasicInfo, type Step1Values } from "@/features/event/components/EventCreateStepper/Step1BasicInfo";
import { Step2Content } from "@/features/event/components/EventCreateStepper/Step2Content";
import { Step3Images } from "@/features/event/components/EventCreateStepper/Step3Images";
import { Step4Tickets } from "@/features/event/components/EventCreateStepper/Step4Tickets";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./page.module.css";

const INITIAL_VALUES: Step1Values = {
  title: "",
  hostName: "",
  categoryId: "",
  manager: null,
  startDate: "",
  endDate: "",
  venueName: "",
  address: "",
  detailAddress: "",
  latitude: null,
  longitude: null,
  kakaoPlaceId: null,
  legalDongCode: null,
};

export default function NewEventPage() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [maxReachedStep, setMaxReachedStep] = useState(1);
  const [eventId, setEventId] = useState<string | null>(null);
  const [values, setValues] = useState<Step1Values>(INITIAL_VALUES);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [createDraftEvent, { isLoading: isCreating }] = useCreateDraftEventMutation();
  const [updateEvent, { isLoading: isUpdating }] = useUpdateAdminEventMutation();
  const [changeManager, { isLoading: isChangingManager }] = useChangeEventManagerMutation();
  const { data: detail } = useGetAdminEventDetailQuery(eventId ?? "", { skip: !eventId });

  const updateValues = (patch: Partial<Step1Values>) => setValues((prev) => ({ ...prev, ...patch }));

  const goToStep = (target: number) => {
    if (target <= maxReachedStep) setStep(target);
  };

  const goNextFrom = (current: number) => {
    const next = current + 1;
    setStep(next);
    setMaxReachedStep((prev) => Math.max(prev, next));
  };

  const goBackFrom = (current: number) => setStep(Math.max(current - 1, 1));

  const buildUpdatePayload = () => ({
    title: values.title,
    categoryId: values.categoryId,
    hostName: values.hostName || null,
    venueName: values.venueName || null,
    address: values.address || null,
    detailAddress: values.detailAddress || null,
    kakaoPlaceId: values.kakaoPlaceId,
    legalDongCode: values.legalDongCode,
    latitude: values.latitude,
    longitude: values.longitude,
    startDate: values.startDate || null,
    endDate: values.endDate || null,
  });

  const handleStep1Next = async () => {
    setSubmitError(null);
    try {
      if (eventId) {
        // 이미 생성된 행사로 되돌아온 경우 - 값 수정을 반영한다.
        await updateEvent({ eventId, payload: buildUpdatePayload() }).unwrap();
        if (values.manager && detail && values.manager.id !== detail.managerId) {
          await changeManager({ eventId, managerId: values.manager.id }).unwrap();
        }
        goNextFrom(1);
        return;
      }
      const created = await createDraftEvent({
        title: values.title,
        categoryId: values.categoryId,
        managerId: values.manager!.id,
      }).unwrap();
      await updateEvent({ eventId: created.id, payload: buildUpdatePayload() }).unwrap();
      setEventId(created.id);
      goNextFrom(1);
    } catch (reason) {
      setSubmitError(queryErrorMessage(reason, "저장에 실패했습니다."));
    }
  };

  const handleComplete = () => {
    if (eventId) router.push(`/manager/events/${eventId}`);
  };

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>새 행사 등록</h1>
      </header>

      <StepIndicator currentStep={step} maxReachedStep={maxReachedStep} onStepClick={goToStep} />

      {step === 1 && (
        <Step1BasicInfo
          values={values}
          onChange={updateValues}
          onNext={() => void handleStep1Next()}
          isSubmitting={isCreating || isUpdating || isChangingManager}
          submitError={submitError}
        />
      )}
      {step === 2 && eventId && (
        <Step2Content
          eventId={eventId}
          initialContents={detail?.contents ?? []}
          onBack={() => goBackFrom(2)}
          onNext={() => goNextFrom(2)}
        />
      )}
      {step === 3 && eventId && (
        <Step3Images
          eventId={eventId}
          images={detail?.images ?? []}
          onBack={() => goBackFrom(3)}
          onNext={() => goNextFrom(3)}
        />
      )}
      {step === 4 && eventId && (
        <Step4Tickets
          eventId={eventId}
          tickets={detail?.tickets ?? []}
          onBack={() => goBackFrom(4)}
          onComplete={handleComplete}
        />
      )}
    </section>
  );
}