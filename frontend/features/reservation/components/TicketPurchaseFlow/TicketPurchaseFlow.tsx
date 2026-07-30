"use client";

import { useState } from "react";
import { CheckCircle2, Ticket as TicketIcon } from "lucide-react";
import { WaitingRoomPanel } from "../WaitingRoomPanel";
import { TicketSelectionPanel } from "../TicketSelectionPanel";
import type { TicketSelection, TicketTypeOption } from "../TicketSelectionPanel";
import {
  useCreateReservationOrderMutation,
  useEnterWaitingRoomMutation,
  useGetWaitingRoomStatusQuery,
} from "../../api/reservationApi";
import type { OrderDetail } from "../../types/reservation";
import styles from "./TicketPurchaseFlow.module.css";

type Phase = "idle" | "waiting" | "submitting" | "success";

interface TicketPurchaseFlowProps {
  eventId: string;
  ticketTypes: TicketTypeOption[];
}

// 서버가 30초 넘게 폴링 없으면 대기 티켓을 자동 만료시키므로(WaitingRoomService.expireStaleTickets),
// 그보다 훨씬 짧은 주기로 폴링해야 한다.
const POLL_INTERVAL_MS = 4000;

export function TicketPurchaseFlow({ eventId, ticketTypes }: TicketPurchaseFlowProps) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<OrderDetail | null>(null);

  const [enterWaitingRoom, { isLoading: isEntering }] = useEnterWaitingRoomMutation();
  const [createOrder, { isLoading: isCreatingOrder }] = useCreateReservationOrderMutation();

  // waiting/submitting 동안은 구독을 계속 살려둬서(스킵하지 않아서) 폴링 결과가 항상 최신 상태를 반영하게 한다.
  const { data: waitingStatus } = useGetWaitingRoomStatusQuery(eventId, {
    skip: phase === "idle" || phase === "success",
    pollingInterval: POLL_INTERVAL_MS,
  });

  const isAdmitted = waitingStatus?.status === "ADMITTED";
  const isExpired = phase === "waiting" && waitingStatus?.status === "EXPIRED";

  async function handleStart() {
    setError(null);
    try {
      await enterWaitingRoom(eventId).unwrap();
      setPhase("waiting");
    } catch (e) {
      setError(e instanceof Error ? e.message : "대기열 입장에 실패했습니다.");
    }
  }

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
      setPhase("waiting");
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
        <button type="button" className={styles.purchaseButton} onClick={handleStart} disabled={isEntering}>
          <TicketIcon size={18} />
          {isEntering ? "입장 중..." : "티켓 구매하기"}
        </button>
      )}

      {phase === "waiting" && !isAdmitted && (
        <div className={styles.card}>
          {isExpired ? (
            <div className={styles.expiredPanel}>
              <p className={styles.expiredText}>대기가 만료되었습니다. 다시 시도해주세요.</p>
              <button type="button" className={styles.resetButton} onClick={() => setPhase("idle")}>
                처음으로
              </button>
            </div>
          ) : (
            <WaitingRoomPanel position={waitingStatus?.position ?? null} />
          )}
        </div>
      )}

      {((phase === "waiting" && isAdmitted) || phase === "submitting") && (
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
