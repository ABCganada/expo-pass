"use client";

import { useState } from "react";
import { Trash2 } from "lucide-react";
import {
  useCreatePolicyMutation,
  useDeletePolicyMutation,
  useGetPoliciesBySlotQuery,
} from "../../api/adminBannerApi";
import type { BannerSlot } from "../../types/marketerBanner";
import styles from "./SlotPolicyModal.module.css";

interface SlotPolicyModalProps {
  slot: BannerSlot;
  onClose: () => void;
}

export function SlotPolicyModal({ slot, onClose }: SlotPolicyModalProps) {
  const { data: policies = [], isLoading } = useGetPoliciesBySlotQuery(slot.id);
  const [createPolicy, { isLoading: creating }] = useCreatePolicyMutation();
  const [deletePolicy] = useDeletePolicyMutation();

  const [durationDays, setDurationDays] = useState(7);
  const [price, setPrice] = useState(0);
  const [error, setError] = useState("");

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      await createPolicy({ slotId: slot.id, durationDays, price }).unwrap();
      setDurationDays(7);
      setPrice(0);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "정책 추가에 실패했습니다.");
    }
  };

  const handleDelete = async (policyId: string) => {
    if (!confirm("이 가격 정책을 삭제하시겠습니까?")) return;
    try {
      await deletePolicy({ slotId: slot.id, policyId }).unwrap();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "정책 삭제에 실패했습니다.");
    }
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>가격 정책 관리</h2>
            <p className={styles.slotName}>{slot.name}</p>
          </div>
          <button type="button" className={styles.btnClose} onClick={onClose} aria-label="닫기">✕</button>
        </div>

        {/* 기존 정책 목록 */}
        <div className={styles.policyList}>
          {isLoading && <p className={styles.state}>불러오는 중...</p>}
          {!isLoading && policies.length === 0 && (
            <p className={styles.empty}>등록된 가격 정책이 없습니다.</p>
          )}
          {policies.map((policy) => (
            <div key={policy.id} className={styles.policyRow}>
              <span className={styles.policyDays}>{policy.durationDays}일</span>
              <span className={styles.policyPrice}>
                {policy.price.toLocaleString("ko-KR")}원
              </span>
              <button
                type="button"
                className={styles.btnDelete}
                onClick={() => handleDelete(policy.id)}
                aria-label="정책 삭제"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
        </div>

        <hr className={styles.divider} />

        {/* 정책 추가 폼 */}
        <form className={styles.addForm} onSubmit={handleCreate}>
          <p className={styles.addTitle}>정책 추가</p>
          <div className={styles.addRow}>
            <div className={styles.addField}>
              <label className={styles.addLabel}>기간 (일)</label>
              <input
                type="number"
                className={styles.input}
                value={durationDays}
                onChange={(e) => setDurationDays(Number(e.target.value))}
                min={1}
                max={365}
                required
              />
            </div>
            <div className={styles.addField}>
              <label className={styles.addLabel}>금액 (원)</label>
              <input
                type="number"
                className={styles.input}
                value={price}
                onChange={(e) => setPrice(Number(e.target.value))}
                min={0}
                required
              />
            </div>
            <button type="submit" className={styles.btnAdd} disabled={creating}>
              {creating ? "추가 중..." : "추가"}
            </button>
          </div>
          {error && <p className={styles.error}>{error}</p>}
        </form>
      </div>
    </div>
  );
}
