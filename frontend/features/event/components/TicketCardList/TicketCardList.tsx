"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import type { EventTicket } from "../../types/eventManagementDetail";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { TicketCard } from "./TicketCard";
import { TicketCardEditForm } from "./TicketCardEditForm";
import { useTicketMutations } from "./useTicketMutations";
import { EMPTY_DRAFT, draftFromTicket, type TicketDraft } from "./ticketUtils";
import styles from "./TicketCardList.module.css";

interface TicketCardListProps {
  eventId: string;
  tickets: EventTicket[];
}

export function TicketCardList({ eventId, tickets }: TicketCardListProps) {
  const { create, update, remove, isCreating, isUpdating, error, setError } = useTicketMutations(eventId);

  const [editingTicketId, setEditingTicketId] = useState<string | "new" | null>(null);
  const [draft, setDraft] = useState<TicketDraft>(EMPTY_DRAFT);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  const startCreate = () => {
    setEditingTicketId("new");
    setDraft({ ...EMPTY_DRAFT });
    setError(null);
  };

  const startEdit = (ticket: EventTicket) => {
    setEditingTicketId(ticket.id);
    setDraft(draftFromTicket(ticket));
    setError(null);
  };

  const cancelEdit = () => {
    setEditingTicketId(null);
    setError(null);
  };

  const handleSave = async () => {
    if (editingTicketId === null) return;
    if (
        !draft.name.trim() ||
        !draft.price ||
        !draft.quantityTotal ||
        !draft.maxPurchasePerUser ||
        !draft.saleStartAt ||
        !draft.saleEndAt
    ) {
      setError("필수 항목을 입력해주세요.");
      return;
    }
    const ok = editingTicketId === "new" ? await create(draft) : await update(editingTicketId, draft);
    if (ok) setEditingTicketId(null);
  };

  const handleConfirmDelete = async () => {
    if (!pendingDeleteId) return;
    const ok = await remove(pendingDeleteId);
    if (ok) setPendingDeleteId(null);
  };

  return (
    <div className={styles.wrapper}>
      <button type="button" className={styles.addButton} onClick={startCreate} disabled={editingTicketId !== null}>
        <Plus size={16} />+ 티켓 추가
      </button>

      {error && <p className={styles.error}>{error}</p>}

      <div className={styles.cardList}>
        {editingTicketId === "new" && (
          <TicketCardEditForm
            draft={draft}
            onChange={(patch) => setDraft((prev) => ({ ...prev, ...patch }))}
            isNew
            originalSaleStartAt={null}
            isSaving={isCreating}
            onSave={() => void handleSave()}
            onCancel={cancelEdit}
          />
        )}

        {tickets.map((ticket) =>
          editingTicketId === ticket.id ? (
            <TicketCardEditForm
              key={ticket.id}
              draft={draft}
              onChange={(patch) => setDraft((prev) => ({ ...prev, ...patch }))}
              isNew={false}
              originalSaleStartAt={ticket.saleStartAt}
              isSaving={isUpdating}
              onSave={() => void handleSave()}
              onCancel={cancelEdit}
            />
          ) : (
            <TicketCard
              key={ticket.id}
              ticket={ticket}
              disabled={editingTicketId !== null}
              onEdit={() => startEdit(ticket)}
              onDelete={() => setPendingDeleteId(ticket.id)}
            />
          ),
        )}

        {tickets.length === 0 && editingTicketId !== "new" && <p className={styles.empty}>등록된 티켓이 없습니다.</p>}
      </div>

      {pendingDeleteId && (
        <ConfirmDialog
          title="티켓을 삭제할까요?"
          description="삭제하면 되돌릴 수 없습니다."
          confirmLabel="삭제"
          danger
          onConfirm={() => void handleConfirmDelete()}
          onCancel={() => setPendingDeleteId(null)}
        />
      )}
    </div>
  );
}