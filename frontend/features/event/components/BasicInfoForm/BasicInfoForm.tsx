"use client";

import { useState } from "react";
import { useGetEventCategoriesQuery } from "../../api/eventApi";
import { ManagerPicker, type ManagerOption } from "../ManagerPicker/ManagerPicker";
import { VenueSearchInput, type VenueSelection } from "../VenueSearchInput/VenueSearchInput";
import { DatePicker } from "../DatePicker/DatePicker";
import { StatusFilterDropdown } from "../StatusFilterDropdown/StatusFilterDropdown";
import { useAuth } from "@/features/auth/hooks/useAuth";
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
  legalDongCode: string | null;
}

interface BasicInfoFormProps {
  values: BasicInfoFormValues;
  onChange: (patch: Partial<BasicInfoFormValues>) => void;
  onValidSubmit: () => void;
  readOnly?: boolean;
  footer: React.ReactNode;
}

function displayText(value: string): string {
  return value.trim() === "" ? "-" : value;
}

function displayDate(value: string): string {
  return value ? value.replaceAll("-", ".") : "-";
}

function displayManager(manager: ManagerOption | null): string {
  if (!manager) return "-";
  return manager.email ? `${manager.name} (${manager.email})` : manager.name;
}

/**
 * 행사명/주최자·카테고리/담당자/기간/장소/주소 필드를 조회·수정 모드 공용 레이아웃으로 렌더링
 * readOnly일 때는 동일한 자리에 값만 표시하고, 아닐 때는 실제 입력 컴포넌트로 전환
 */
export function BasicInfoForm({ values, onChange, onValidSubmit, readOnly = false, footer }: BasicInfoFormProps) {
  const { data: categories = [] } = useGetEventCategoriesQuery();
  const { user } = useAuth();
  const isAdmin = user?.roles.includes("ADMIN") ?? false;
  const managerReadOnly = readOnly || !isAdmin;
  const [invalid, setInvalid] = useState<{ title?: boolean; categoryId?: boolean; manager?: boolean }>({});

  const categoryOptions = categories.map((category) => ({ value: category.id, label: category.name }));
  const categoryLabel = categoryOptions.find((option) => option.value === values.categoryId)?.label ?? "-";

  const handleVenueSelect = (venue: VenueSelection) => {
    onChange({
      venueName: venue.venueName,
      address: venue.address,
      latitude: venue.latitude,
      longitude: venue.longitude,
      kakaoPlaceId: venue.kakaoPlaceId,
      legalDongCode: venue.legalDongCode,
    });
  };

  const handleVenueClear = () => {
    onChange({
      venueName: "",
      address: "",
      latitude: null,
      longitude: null,
      kakaoPlaceId: null,
      legalDongCode: null,
    });
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (readOnly) return;
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
        {readOnly ? (
          <div className={styles.readonlyBox}>{displayText(values.title)}</div>
        ) : (
          <>
            <input
              value={values.title}
              onChange={(event) => onChange({ title: event.target.value })}
              data-invalid={invalid.title}
            />
            {invalid.title && <p className={styles.fieldError}>필수 정보입니다.</p>}
          </>
        )}
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주최자명</span>
          {readOnly ? (
            <div className={styles.readonlyBox}>{displayText(values.hostName)}</div>
          ) : (
            <input value={values.hostName} onChange={(event) => onChange({ hostName: event.target.value })} />
          )}
        </label>
        <label className={styles.field}>
          <span>
            카테고리 <em className={styles.required}>*</em>
          </span>
          {readOnly ? (
            <div className={styles.readonlyBox}>{categoryLabel}</div>
          ) : (
            <>
              <StatusFilterDropdown
                options={categoryOptions}
                value={values.categoryId}
                onChange={(categoryId) => onChange({ categoryId })}
              />
              {invalid.categoryId && <p className={styles.fieldError}>필수 정보입니다.</p>}
            </>
          )}
        </label>
      </div>

      <label className={styles.fieldFull}>
        <span>
          담당자 <em className={styles.required}>*</em>
        </span>
        {managerReadOnly ? (
          <div className={styles.readonlyBox}>{displayManager(values.manager)}</div>
        ) : (
          <>
            <ManagerPicker
              selectedManager={values.manager}
              onSelect={(manager) => onChange({ manager })}
              onClear={() => onChange({ manager: null })}
              invalid={invalid.manager}
            />
            {invalid.manager && <p className={styles.fieldError}>필수 정보입니다.</p>}
          </>
        )}
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>시작일</span>
          {readOnly ? (
            <div className={styles.readonlyBox}>{displayDate(values.startDate)}</div>
          ) : (
            <DatePicker value={values.startDate} onChange={(startDate) => onChange({ startDate })} />
          )}
        </label>
        <label className={styles.field}>
          <span>종료일</span>
          {readOnly ? (
            <div className={styles.readonlyBox}>{displayDate(values.endDate)}</div>
          ) : (
            <DatePicker value={values.endDate} onChange={(endDate) => onChange({ endDate })} />
          )}
        </label>
      </div>

      <label className={styles.fieldFull}>
        <span>장소명</span>
        {readOnly ? (
          <div className={styles.readonlyBox}>{displayText(values.venueName)}</div>
        ) : (
          <VenueSearchInput
            selectedVenueName={values.venueName}
            onSelect={handleVenueSelect}
            onClear={handleVenueClear}
          />
        )}
      </label>

      <div className={styles.fieldRow}>
        <label className={styles.field}>
          <span>주소</span>
          <div className={styles.readonlyBox}>{values.address || (readOnly ? "-" : "주소를 입력하세요")}</div>
        </label>
        <label className={styles.field}>
          <span>상세주소</span>
          {readOnly ? (
            <div className={styles.readonlyBox}>{displayText(values.detailAddress)}</div>
          ) : (
            <input
              value={values.detailAddress}
              onChange={(event) => onChange({ detailAddress: event.target.value })}
              placeholder="상세주소 (선택)"
            />
          )}
        </label>
      </div>

      {footer}
    </form>
  );
}