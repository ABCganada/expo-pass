"use client";

import { useState } from "react";
import { ClipboardCheck, Minus, Plus } from "lucide-react";
import styles from "./TicketSelectionPanel.module.css";

// TODO: Event 도메인 API 연결 시 이 컴포넌트를 호출하는 쪽(행사 상세 페이지)에서
// 실제 티켓 종류(TicketInfo 기반)를 props로 내려준다.
export interface TicketTypeOption {
  ticketId: string;
  name: string;
  price: number;
  maxPerUser: number;
}

export interface TicketSelection {
  ticketId: string;
  quantity: number;
}

interface TicketSelectionPanelProps {
  ticketTypes: TicketTypeOption[];
  isSubmitting: boolean;
  onSubmit: (selections: TicketSelection[]) => void;
}

function formatAmount(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export function TicketSelectionPanel({ ticketTypes, isSubmitting, onSubmit }: TicketSelectionPanelProps) {
  const [quantities, setQuantities] = useState<Record<string, number>>({});

  const updateQuantity = (ticketId: string, delta: number, max: number) => {
    setQuantities((prev) => {
      const next = Math.min(max, Math.max(0, (prev[ticketId] ?? 0) + delta));
      return { ...prev, [ticketId]: next };
    });
  };

  const totalQuantity = Object.values(quantities).reduce((sum, q) => sum + q, 0);
  const totalAmount = ticketTypes.reduce(
    (sum, ticket) => sum + ticket.price * (quantities[ticket.ticketId] ?? 0),
    0,
  );

  const handleSubmit = () => {
    const selections = ticketTypes
      .map((ticket) => ({ ticketId: ticket.ticketId, quantity: quantities[ticket.ticketId] ?? 0 }))
      .filter((selection) => selection.quantity > 0);
    onSubmit(selections);
  };

  return (
    <div className={styles.panel}>
      <p className={styles.title}>입장 순서가 되었습니다. 구매할 티켓을 선택해주세요.</p>

      <div className={styles.ticketList}>
        {ticketTypes.map((ticket) => {
          const quantity = quantities[ticket.ticketId] ?? 0;
          return (
            <div key={ticket.ticketId} className={styles.ticketRow}>
              <div className={styles.ticketInfo}>
                <span className={styles.ticketName}>{ticket.name}</span>
                <span className={styles.ticketPrice}>{formatAmount(ticket.price)}</span>
              </div>
              <div className={styles.stepper}>
                <button
                  type="button"
                  className={styles.stepperButton}
                  aria-label={`${ticket.name} 수량 감소`}
                  onClick={() => updateQuantity(ticket.ticketId, -1, ticket.maxPerUser)}
                  disabled={quantity === 0}
                >
                  <Minus size={16} />
                </button>
                <span className={styles.stepperValue}>{quantity}</span>
                <button
                  type="button"
                  className={styles.stepperButton}
                  aria-label={`${ticket.name} 수량 증가`}
                  onClick={() => updateQuantity(ticket.ticketId, 1, ticket.maxPerUser)}
                  disabled={quantity >= ticket.maxPerUser}
                >
                  <Plus size={16} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <div className={styles.summary}>
        <span className={styles.summaryLabel}>총 {totalQuantity}매</span>
        <span className={styles.summaryAmount}>{formatAmount(totalAmount)}</span>
      </div>

      <button
        type="button"
        className={styles.submitButton}
        disabled={totalQuantity === 0 || isSubmitting}
        onClick={handleSubmit}
      >
        <ClipboardCheck size={18} />
        {isSubmitting ? "예약 신청 중..." : "예약 신청하기"}
      </button>
    </div>
  );
}
