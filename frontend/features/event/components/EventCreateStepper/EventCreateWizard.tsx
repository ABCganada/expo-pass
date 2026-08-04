"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { useCreateManagerEventMutation } from "@/features/event/api/managerEventApi";
import { useGetManagerEventDetailQuery, useUpdateManagerEventMutation } from "@/features/event/api/managerEventDetailApi";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { StepIndicator } from "./StepIndicator";
import { Step1BasicInfo, type Step1Values } from "./Step1BasicInfo";
import { Step2Content } from "./Step2Content";
import { Step3Images } from "./Step3Images";
import { Step4Tickets } from "./Step4Tickets";
import styles from "./EventCreateWizard.module.css";

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

/** 행사 등록 4단계 위저드. 항상 manager 본인 소유 행사로 self-assign 생성한다. */
export function EventCreateWizard() {
  const router = useRouter();
  const { user } = useAuth();
  const [step, setStep] = useState(1);
  const [maxReachedStep, setMaxReachedStep] = useState(1);
  const [eventId, setEventId] = useState<string | null>(null);
  const [values, setValues] = useState<Step1Values>(INITIAL_VALUES);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // 담당자는 항상 작성자 본인으로 self-assign. 로그인 정보가 늦게 로드되는 경우를 대비해 값이 채워지면 동기화한다.
  const [syncedUserId, setSyncedUserId] = useState<string | null>(null);
  if (user && user.id !== syncedUserId) {
    setSyncedUserId(user.id);
    setValues((prev) => ({ ...prev, manager: { id: user.id, name: user.name, email: user.email } }));
  }

  const [createDraftEvent, { isLoading: isCreating }] = useCreateManagerEventMutation();
  const [updateEvent, { isLoading: isUpdating }] = useUpdateManagerEventMutation();
  const { data: detail } = useGetManagerEventDetailQuery(eventId ?? "", { skip: !eventId });

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
        goNextFrom(1);
        return;
      }
      const created = await createDraftEvent({ title: values.title, categoryId: values.categoryId }).unwrap();
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
      <button type="button" className={styles.backButton} onClick={() => router.push("/manager/events")}>
        <ArrowLeft size={16} />
        목록으로
      </button>

      <StepIndicator currentStep={step} maxReachedStep={maxReachedStep} onStepClick={goToStep} />

      {step === 1 && (
        <Step1BasicInfo
          values={values}
          onChange={updateValues}
          onNext={() => void handleStep1Next()}
          isSubmitting={isCreating || isUpdating}
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