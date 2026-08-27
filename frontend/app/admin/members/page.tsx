"use client";

import { useEffect, useMemo, useState } from "react";
import {
  useGetMembersQuery,
  useUpdateMemberRolesMutation,
} from "@/features/admin/api/memberApi";
import { ADMIN_ROLES, type AdminMember, type AdminRole } from "@/features/admin/types/member";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./page.module.css";

const PAGE_SIZE = 20;

function sameRoles(left: AdminRole[], right: AdminRole[]): boolean {
  if (left.length !== right.length) return false;
  const rightSet = new Set(right);
  return left.every((role) => rightSet.has(role));
}

export default function MembersPage() {
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [edits, setEdits] = useState<Record<string, AdminRole[]>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const [rowError, setRowError] = useState<Record<string, string>>({});

  const { user } = useAuth();
  const currentUserId = user?.id ?? "";

  const [updateRoles] = useUpdateMemberRolesMutation();

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQuery(searchInput.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  const { data, isLoading, isFetching, isError, error } = useGetMembersQuery({
    query,
    page,
    size: PAGE_SIZE,
  });

  const members = useMemo(() => data?.members ?? [], [data]);

  const effectiveRoles = (member: AdminMember): AdminRole[] => edits[member.id] ?? member.roles;

  const isDirty = (member: AdminMember): boolean =>
    edits[member.id] !== undefined && !sameRoles(edits[member.id], member.roles);

  // USER 는 모두 상시 보유. 본인의 ADMIN 은 스스로 해제할 수 없다(마지막 관리자 자물쇠).
  const isLocked = (member: AdminMember, role: AdminRole): boolean =>
    role === "USER" || (role === "ADMIN" && member.id === currentUserId);

  const toggleRole = (member: AdminMember, role: AdminRole) => {
    if (isLocked(member, role)) return;
    const current = effectiveRoles(member);
    const next = current.includes(role)
      ? current.filter((value) => value !== role)
      : [...current, role];
    const withUser: AdminRole[] = next.includes("USER") ? next : [...next, "USER" as AdminRole];
    setEdits((previous) => ({ ...previous, [member.id]: withUser }));
  };

  const resetRow = (member: AdminMember) => {
    setEdits((previous) => {
      const next = { ...previous };
      delete next[member.id];
      return next;
    });
    setRowError((previous) => ({ ...previous, [member.id]: "" }));
  };

  const saveRow = async (member: AdminMember) => {
    setSavingId(member.id);
    setRowError((previous) => ({ ...previous, [member.id]: "" }));
    try {
      await updateRoles({ id: member.id, roles: effectiveRoles(member) }).unwrap();
      setEdits((previous) => {
        const next = { ...previous };
        delete next[member.id];
        return next;
      });
    } catch (reason) {
      setRowError((previous) => ({
        ...previous,
        [member.id]: queryErrorMessage(reason, "권한 변경에 실패했습니다."),
      }));
    } finally {
      setSavingId(null);
    }
  };

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>회원 관리</h1>
          <p className={styles.subtitle}>전체 회원의 권한을 설정합니다. 모든 회원은 USER 권한을 기본으로 보유합니다.</p>
        </div>
        <input
          type="search"
          className={styles.search}
          placeholder="이름 또는 이메일 검색"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
        />
      </header>

      {isError ? (
        <div className={styles.state}>{queryErrorMessage(error, "회원 목록을 불러오지 못했습니다.")}</div>
      ) : isLoading ? (
        <div className={styles.state}>불러오는 중...</div>
      ) : members.length === 0 ? (
        <div className={styles.state}>표시할 회원이 없습니다.</div>
      ) : (
        <div className={styles.tableWrap} data-fetching={isFetching}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th className={styles.memberCol}>회원</th>
                <th className={styles.statusCol}>상태</th>
                {ADMIN_ROLES.map((role) => (
                  <th key={role} className={styles.roleCol}>{role}</th>
                ))}
                <th className={styles.actionCol}>작업</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => {
                const roles = effectiveRoles(member);
                const dirty = isDirty(member);
                const saving = savingId === member.id;
                return (
                  <tr key={member.id} data-dirty={dirty}>
                    <td>
                      <div className={styles.memberName}>
                        {member.name || "(이름 없음)"}
                        {member.id === currentUserId ? (
                          <span className={styles.selfBadge} title="본인 계정 (ADMIN 해제 불가)">
                            나
                          </span>
                        ) : null}
                      </div>
                      <div className={styles.memberEmail}>{member.email}</div>
                      {rowError[member.id] ? (
                        <div className={styles.rowError}>{rowError[member.id]}</div>
                      ) : null}
                    </td>
                    <td>
                      <span
                        className={styles.statusBadge}
                        data-status={member.status}
                      >
                        {member.status === "ACTIVE" ? "활성" : "비활성"}
                      </span>
                    </td>
                    {ADMIN_ROLES.map((role) => (
                      <td key={role} className={styles.roleCell}>
                        <input
                          type="checkbox"
                          checked={roles.includes(role)}
                          disabled={isLocked(member, role) || saving}
                          onChange={() => toggleRole(member, role)}
                          aria-label={`${member.email} ${role}`}
                        />
                      </td>
                    ))}
                    <td className={styles.actionCell}>
                      <button
                        type="button"
                        className={styles.saveButton}
                        disabled={!dirty || saving}
                        onClick={() => saveRow(member)}
                      >
                        {saving ? "저장 중" : "저장"}
                      </button>
                      {dirty ? (
                        <button
                          type="button"
                          className={styles.resetButton}
                          disabled={saving}
                          onClick={() => resetRow(member)}
                        >
                          취소
                        </button>
                      ) : null}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 ? (
        <footer className={styles.pagination}>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page <= 0 || isFetching}
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
          >
            이전
          </button>
          <span className={styles.pageInfo}>
            {page + 1} / {totalPages} (총 {totalElements}명)
          </span>
          <button
            type="button"
            className={styles.pageButton}
            disabled={page + 1 >= totalPages || isFetching}
            onClick={() => setPage((value) => value + 1)}
          >
            다음
          </button>
        </footer>
      ) : null}
    </section>
  );
}
