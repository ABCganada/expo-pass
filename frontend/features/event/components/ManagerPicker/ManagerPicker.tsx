"use client";

import { useEffect, useState } from "react";
import { Search, X } from "lucide-react";
import { useGetMembersQuery } from "@/features/admin/api/memberApi";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./ManagerPicker.module.css";

export interface ManagerOption {
  id: string;
  name: string;
  email: string;
}

interface ManagerPickerProps {
  selectedManager: ManagerOption | null;
  onSelect: (manager: ManagerOption) => void;
  onClear: () => void;
  invalid?: boolean;
}

export function ManagerPicker({ selectedManager, onSelect, onClear, invalid }: ManagerPickerProps) {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), query ? 250 : 0);
    return () => window.clearTimeout(timer);
  }, [query]);

  const { data, isLoading, isFetching, error } = useGetMembersQuery({ query: debouncedQuery, page: 0, size: 20 });
  const managers = (data?.members ?? []).filter(
    (member) => member.roles.includes("MANAGER") && member.status === "ACTIVE",
  );
  const loading = query !== debouncedQuery || isLoading || isFetching;

  if (selectedManager) {
    return (
      <div className={styles.chip}>
        <span>
          {selectedManager.name} ({selectedManager.email})
        </span>
        <button type="button" className={styles.clearButton} onClick={onClear} aria-label="담당자 선택 해제">
          <X size={18} strokeWidth={2.5} />
        </button>
      </div>
    );
  }

  return (
    <div className={styles.picker} data-invalid={invalid}>
      <div className={styles.searchBox}>
        <Search size={16} aria-hidden />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="담당자 이름 또는 이메일 검색"
          autoComplete="off"
        />
      </div>
      <div className={styles.results}>
        {loading ? (
          <p className={styles.hint}>검색 중...</p>
        ) : error ? (
          <p className={styles.hint}>{queryErrorMessage(error, "담당자 목록을 불러오지 못했습니다.")}</p>
        ) : managers.length === 0 ? (
          <p className={styles.hint}>MANAGER 권한을 가진 활성 계정이 없습니다.</p>
        ) : (
          managers.map((manager) => (
            <button
              key={manager.id}
              type="button"
              className={styles.option}
              onClick={() => onSelect({ id: manager.id, name: manager.name, email: manager.email })}
            >
              <strong>{manager.name || "(이름 없음)"}</strong>
              <span>{manager.email}</span>
            </button>
          ))
        )}
      </div>
    </div>
  );
}