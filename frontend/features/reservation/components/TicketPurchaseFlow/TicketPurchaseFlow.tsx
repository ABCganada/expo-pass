"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CheckCircle2, Ticket as TicketIcon, Users } from "lucide-react";
import { PaymentCheckoutButton } from "@/features/payment/components/PaymentCheckoutButton";
import { TicketSelectionPanel } from "../TicketSelectionPanel";
import type { TicketSelection, TicketTypeOption } from "../TicketSelectionPanel";
import { useCreateReservationOrderMutation } from "../../api/reservationApi";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { useWaitingRoom } from "../../hooks/useWaitingRoom";
import type { OrderDetail } from "../../types/reservation";
import styles from "./TicketPurchaseFlow.module.css";

type Phase = "idle" | "waiting" | "selecting" | "submitting" | "redirecting" | "confirmed";

interface TicketPurchaseFlowProps {
  eventId: string;
  ticketTypes: TicketTypeOption[];
}

/** 토스 결제창에 표시할 주문명. 티켓 종류가 여러 개면 "A 외 N건"으로 축약한다. */
function buildOrderName(order: OrderDetail, ticketTypes: TicketTypeOption[]): string {
  const ticketIds = Array.from(new Set(order.items.map((item) => item.ticketId)));
  const firstName = ticketTypes.find((t) => t.ticketId === ticketIds[0])?.name ?? "티켓";
  return ticketIds.length > 1 ? `${firstName} 외 ${ticketIds.length - 1}건` : `${firstName} ${order.items.length}매`;
}

export function TicketPurchaseFlow({ eventId, ticketTypes }: TicketPurchaseFlowProps) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const checkoutWrapRef = useRef<HTMLDivElement>(null);
  const hasAutoClicked = useRef(false);

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
      if (detail.status === "CONFIRMED") {
        // 무료(0원) 주문은 결제 자체가 없어 서버가 바로 CONFIRMED로 만들어준다 — 토스로 갈 필요가 없다.
        setPhase("confirmed");
      } else {
        hasAutoClicked.current = false;
        setPhase("redirecting");
      }
    } catch (e) {
      setError(queryErrorMessage(e, "주문 생성에 실패했습니다."));
      setPhase("selecting");
    }
  }

  // "결제하기"를 누른 순간의 클릭 한 번으로 주문 생성 + 토스 결제창까지 이어지도록,
  // 결제 버튼이 화면에 나타나자마자 자동으로 한 번 클릭해준다(버튼 자체는 그대로 두고
  // 위임만 함 — 결제 SDK 연동 로직은 Payment 도메인의 PaymentCheckoutButton이 캡슐화).
  useEffect(() => {
    if (phase !== "redirecting" || hasAutoClicked.current) return;
    const button = checkoutWrapRef.current?.querySelector("button");
    if (button) {
      hasAutoClicked.current = true;
      button.click();
    }
  }, [phase, order]);

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

      {phase === "redirecting" && order && (
        <div className={styles.card}>
          <div className={styles.waitingPanel}>
            <div ref={checkoutWrapRef}>
              <PaymentCheckoutButton
                orderId={order.orderId}
                amount={order.totalAmount}
                orderName={buildOrderName(order, ticketTypes)}
                successUrl="/reservations/payment/success"
                failUrl="/reservations/payment/fail"
              />
            </div>
          </div>
        </div>
      )}

      {phase === "confirmed" && order && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <div className={styles.successPanel}>
              <CheckCircle2 size={40} className={styles.successIcon} />
              <p className={styles.waitingTitle}>예약이 완료되었습니다</p>
              <p className={styles.waitingRank}>무료 티켓이라 결제 없이 바로 확정됐어요.</p>
              <Link href="/reservations" className={styles.confirmedLink}>
                예약 내역 바로가기
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
