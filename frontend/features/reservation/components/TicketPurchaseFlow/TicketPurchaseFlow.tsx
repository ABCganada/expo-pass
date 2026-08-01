"use client";

import { useState } from "react";
import { CheckCircle2, Ticket as TicketIcon, Users } from "lucide-react";
import { TicketSelectionPanel } from "../TicketSelectionPanel";
import type { TicketSelection, TicketTypeOption } from "../TicketSelectionPanel";
import { useCreateReservationOrderMutation } from "../../api/reservationApi";
import { useWaitingRoom } from "../../hooks/useWaitingRoom";
import type { OrderDetail } from "../../types/reservation";
import styles from "./TicketPurchaseFlow.module.css";

type Phase = "idle" | "waiting" | "selecting" | "submitting" | "success";

interface TicketPurchaseFlowProps {
  eventId: string;
  ticketTypes: TicketTypeOption[];
}

export function TicketPurchaseFlow({ eventId, ticketTypes }: TicketPurchaseFlowProps) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<OrderDetail | null>(null);

  const [createOrder, { isLoading: isCreatingOrder }] = useCreateReservationOrderMutation();
  const { rank, error: waitingError } = useWaitingRoom(eventId, phase === "waiting", () =>
    setPhase("selecting"),
  );

  async function handleSubmit(selections: TicketSelection[]) {
    setError(null);
    setPhase("submitting");
    try {
      const items = selections.map((selection) => {
        const ticket = ticketTypes.find((t) => t.ticketId === selection.ticketId);
        return { ticketId: selection.ticketId, unitPrice: ticket?.price ?? 0, quantity: selection.quantity };
      });
      const detail = await createOrder({ eventId, items }).unwrap();
      setOrder(detail);
      setPhase("success");
    } catch (e) {
      setError(e instanceof Error ? e.message : "주문 생성에 실패했습니다.");
      setPhase("selecting");
    }
  }

  function handleReset() {
    setPhase("idle");
    setOrder(null);
    setError(null);
  }

  return (
    <div className={styles.container}>
      {error && <p className={styles.error}>{error}</p>}

      {phase === "idle" && (
        <button type="button" className={styles.purchaseButton} onClick={() => setPhase("waiting")}>
          <TicketIcon size={18} />
          티켓 구매하기
        </button>
      )}

      {phase === "waiting" && (
        <div className={styles.card}>
          <div className={styles.waitingPanel}>
            <Users size={40} className={styles.waitingIcon} />
            <p className={styles.waitingTitle}>대기열에 접속 중입니다</p>
            <p className={styles.waitingRank}>
              {rank !== null ? `현재 순번: ${rank}번째` : "순번 확인 중..."}
            </p>
            {waitingError && <p className={styles.waitingError}>{waitingError}</p>}
          </div>
        </div>
      )}

      {(phase === "selecting" || phase === "submitting") && (
        <div className={styles.card}>
          <TicketSelectionPanel
            ticketTypes={ticketTypes}
            isSubmitting={phase === "submitting" || isCreatingOrder}
            onSubmit={handleSubmit}
          />
        </div>
      )}

      {phase === "success" && order && (
        <div className={styles.card}>
          <div className={styles.successPanel}>
            <CheckCircle2 size={40} className={styles.successIcon} />
            <p className={styles.successTitle}>예약이 접수되었습니다</p>
            <p className={styles.successDescription}>결제가 확인되면 예약이 확정돼요. 결제대기 상태로 저장됐어요.</p>
            <div className={styles.successDetail}>
              <div className={styles.successRow}>
                <span>예약번호</span>
                <strong>{order.orderId}</strong>
              </div>
              <div className={styles.successRow}>
                <span>티켓 수량</span>
                <strong>{order.items.length}매</strong>
              </div>
              <div className={styles.successRow}>
                <span>결제금액</span>
                <strong>{order.totalAmount.toLocaleString("ko-KR")}원</strong>
              </div>
            </div>

            {/*
              TODO: 실제 결제 진입 버튼은 Payment 도메인이 컴포넌트로 제공하기로 함.
              orderId(+ totalAmount)를 props로 받아서 버튼 렌더링부터 결제창 연동까지
              전부 캡슐화된 컴포넌트를 여기에 그대로 끼워넣으면 됨.
              예: <PaymentCheckoutButton orderId={order.orderId} amount={order.totalAmount} />
            */}

            <button type="button" className={styles.resetButton} onClick={handleReset}>
              닫기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
