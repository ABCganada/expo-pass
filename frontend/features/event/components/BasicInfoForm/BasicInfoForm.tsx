"use client";

import { useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { ManagerPicker, type ManagerOption } from "../ManagerPicker/ManagerPicker";
import { VenueSearchInput, type VenueSelection } from "../VenueSearchInput/VenueSearchInput";
import { DatePicker } from "../DatePicker/DatePicker";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import styles from "./BasicInfoForm.module.css";

export interface BasicInfoFormValues {
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

interface BasicInfoFormProps {
  values: BasicInfoFormValues;
  onChange: (patch: Partial<BasicInfoFormValues>) => void;
  onValidSubmit: () => void;
  footer: React.ReactNode;
}

/**
 * 행사명/주최자·카테고리/담당자/기간/장소/주소 필드 렌더링과 장소 선택·날짜 선택·필수값
 * validation을 전담한다. 값 저장 위치(state)와 실제 제출(API 호출)은 wrapper(Step1BasicInfo,
 * BasicInfoTab)가 맡는다.
 */
export function BasicInfoForm({ values, onChange, onValidSubmit, footer }: BasicInfoFormProps) {
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

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const nextInvalid = {
      title: values.title.trim() === "",
      categoryId: values.categoryId === "",
      manager: values.manager === null,
    };
    setInvalid(nextInvalid);
    if (nextInvalid.title || nextInvalid.categoryId || nextInvalid.manager) return;
    onValidSubmit();
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <label className={styles.fieldFull}>
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

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주최자명</span>
          <input value={values.hostName} onChange={(event) => onChange({ hostName: event.target.value })} />
        </label>
        <label className={styles.field}>
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

      <label className={styles.fieldFull}>
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

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>시작일</span>
          <DatePicker value={values.startDate} onChange={(startDate) => onChange({ startDate })} />
        </label>
        <label className={styles.field}>
          <span>종료일</span>
          <DatePicker value={values.endDate} onChange={(endDate) => onChange({ endDate })} />
        </label>
      </div>

      <label className={styles.fieldFull}>
        <span>장소명</span>
        <VenueSearchInput
          selectedVenueName={values.venueName}
          onSelect={handleVenueSelect}
          onClear={handleVenueClear}
        />
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주소</span>
          <div className={styles.readonlyBox}>{values.address || "주소를 입력하세요"}</div>
        </label>
        <label className={styles.field}>
          <span>상세주소</span>
          <input
            value={values.detailAddress}
            onChange={(event) => onChange({ detailAddress: event.target.value })}
            placeholder="상세주소 (선택)"
          />
        </label>
      </div>

      {footer}
    </form>
  );
}