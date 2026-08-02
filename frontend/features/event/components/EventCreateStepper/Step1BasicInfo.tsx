"use client";

import { useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { ManagerPicker, type ManagerOption } from "../ManagerPicker/ManagerPicker";
import { VenueSearchInput, type VenueSelection } from "../VenueSearchInput/VenueSearchInput";
import { DatePicker } from "../DatePicker/DatePicker";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import formStyles from "../AdminEventDetail/AdminEventDetail.module.css";
import styles from "./EventCreateStepper.module.css";

export interface Step1Values {
  title: string;
  hostName: string;
  categoryId: string;
  manager: ManagerOption | null;
  startDate: string;
  endDate: string;
  venueName: string;
  address: string;
  detailAddress: string;
  latitude: number | null;
  longitude: number | null;
  kakaoPlaceId: string | null;
}

interface Step1BasicInfoProps {
  values: Step1Values;
  onChange: (patch: Partial<Step1Values>) => void;
  onNext: () => void;
  isSubmitting: boolean;
  submitError: string | null;
}

export function Step1BasicInfo({ values, onChange, onNext, isSubmitting, submitError }: Step1BasicInfoProps) {
  const { data: categories = [] } = useGetEventCategoriesQuery();
  const [invalid, setInvalid] = useState<{ title?: boolean; categoryId?: boolean; manager?: boolean }>({});

  const categoryOptions = categories.map((category) => ({ value: category.id, label: category.name }));

  const handleVenueSelect = (venue: VenueSelection) => {
    onChange({
      venueName: venue.venueName,
      address: venue.address,
      latitude: venue.latitude,
      longitude: venue.longitude,
      kakaoPlaceId: venue.kakaoPlaceId,
    });
  };

  const handleVenueClear = () => {
    onChange({ venueName: "", address: "", latitude: null, longitude: null, kakaoPlaceId: null });
  };

  const handleNext = () => {
    const nextInvalid = {
      title: values.title.trim() === "",
      categoryId: values.categoryId === "",
      manager: values.manager === null,
    };
    setInvalid(nextInvalid);
    if (nextInvalid.title || nextInvalid.categoryId || nextInvalid.manager) return;
    onNext();
  };

  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <form
          className={formStyles.form}
          onSubmit={(event) => {
            event.preventDefault();
            handleNext();
          }}
        >
          <label className={formStyles.fieldFull}>
            <span>
              행사명 <em className={styles.required}>*</em>
            </span>
            <input
              value={values.title}
              onChange={(event) => onChange({ title: event.target.value })}
              data-invalid={invalid.title}
            />
            {invalid.title && <p className={styles.fieldError}>필수 정보입니다.</p>}
          </label>

          <div className={formStyles.fieldRow}>
            <label className={formStyles.field}>
              <span>주최자명</span>
              <input value={values.hostName} onChange={(event) => onChange({ hostName: event.target.value })} />
            </label>
            <label className={formStyles.field}>
              <span>
                카테고리 <em className={styles.required}>*</em>
              </span>
              <StatusFilterDropdown
                options={categoryOptions}
                value={values.categoryId}
                onChange={(categoryId) => onChange({ categoryId })}
              />
              {invalid.categoryId && <p className={styles.fieldError}>필수 정보입니다.</p>}
            </label>
          </div>

          <label className={formStyles.fieldFull}>
            <span>
              담당자 <em className={styles.required}>*</em>
            </span>
            <ManagerPicker
              selectedManager={values.manager}
              onSelect={(manager) => onChange({ manager })}
              onClear={() => onChange({ manager: null })}
              invalid={invalid.manager}
            />
            {invalid.manager && <p className={styles.fieldError}>필수 정보입니다.</p>}
          </label>

          <div className={formStyles.fieldRow}>
            <label className={formStyles.field}>
              <span>시작일</span>
              <DatePicker value={values.startDate} onChange={(startDate) => onChange({ startDate })} />
            </label>
            <label className={formStyles.field}>
              <span>종료일</span>
              <DatePicker value={values.endDate} onChange={(endDate) => onChange({ endDate })} />
            </label>
          </div>

          <label className={formStyles.fieldFull}>
            <span>장소명</span>
            <VenueSearchInput
              selectedVenueName={values.venueName}
              onSelect={handleVenueSelect}
              onClear={handleVenueClear}
            />
          </label>

          <div className={formStyles.fieldRow}>
            <label className={formStyles.field}>
              <span>주소</span>
              <div className={styles.readonlyBox}>{values.address || "주소를 입력하세요"}</div>
            </label>
            <label className={formStyles.field}>
              <span>상세주소</span>
              <input
                value={values.detailAddress}
                onChange={(event) => onChange({ detailAddress: event.target.value })}
                placeholder="상세주소 (선택)"
              />
            </label>
          </div>

          {submitError && <p className={formStyles.error}>{submitError}</p>}

          <div className={styles.stepFooter}>
            <div className={styles.stepActionsSingle}>
              <button type="submit" className={styles.nextButton} disabled={isSubmitting}>
                {isSubmitting ? "저장 중..." : "다음"}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}