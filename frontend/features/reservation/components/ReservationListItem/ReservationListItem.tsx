"use client";

import { Building2, CalendarDays, ChevronRight, Copy, ExternalLink, UserRound } from "lucide-react";
import { useGetEventForDisplayQuery, useGetEventTicketsForDisplayQuery } from "../../api/eventLookupApi";
import type { OrderStatus, OrderSummary } from "../../types/reservation";
import styles from "./ReservationListItem.module.css";

interface ReservationListItemProps {
  order: OrderSummary;
  onViewEvent?: (eventId: string) => void;
  onViewPayment?: (orderId: string, eventStartDate?: string) => void;
}

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  CONFIRMED: "예약확정",
  CANCELLED: "취소됨",
  REFUNDED: "환불됨",
};

function formatDate(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

export function ReservationListItem({ order, onViewEvent, onViewPayment }: ReservationListItemProps) {
  // 같은 eventId를 쓰는 카드가 여러 개여도 RTK Query 캐시가 묶어줘서 실제 네트워크 요청은 한 번만 나간다.
  const { data: event } = useGetEventForDisplayQuery(order.eventId);
  const { data: tickets = [] } = useGetEventTicketsForDisplayQuery(order.eventId);

  const thumbnailUrl = event?.images.find((image) => image.imageType === "THUMBNAIL")?.imageUrl ?? null;

  const ticketNameById = new Map(tickets.map((ticket) => [ticket.id, ticket.name]));
  const ticketLines = order.ticketQuantities.map((tq) => ({
    label: ticketNameById.get(tq.ticketId) ?? `티켓 #${tq.ticketId}`,
    quantity: tq.quantity,
  }));

  const handleCopy = () => {
    void navigator.clipboard.writeText(order.orderId);
  };

  return (
    <article className={styles.card}>
      <div className={styles.thumbnail}>
        {thumbnailUrl ? (
          <img src={thumbnailUrl} alt="" className={styles.thumbnailImage} />
        ) : (
          <Building2 size={40} />
        )}
      </div>

      <div className={styles.content}>
        <span className={styles.badge} data-status={order.status}>
          {STATUS_LABEL[order.status]}
        </span>
        <h3 className={styles.eventName}>{event?.title ?? "불러오는 중..."}</h3>
        <div className={styles.metaRow}>
          <CalendarDays size={16} />
          <span>{formatDate(order.reservedAt)}</span>
        </div>
        <div className={styles.orderIdRow}>
          <span className={styles.orderIdLabel}>예약번호</span>
          <span className={styles.orderIdValue}>{order.orderId}</span>
          <button type="button" className={styles.copyButton} aria-label="예약번호 복사" onClick={handleCopy}>
            <Copy size={14} />
          </button>
        </div>
      </div>

      <div className={styles.divider} />

      <div className={styles.ticketInfo}>
        {order.totalAmount === 0 && <span className={styles.freeBadge}>무료 티켓</span>}
        <p className={styles.ticketInfoTitle}>티켓 정보</p>
        {ticketLines.map((line) => (
          <div key={line.label} className={styles.ticketLine}>
            <span className={styles.ticketLabel}>
              <UserRound size={16} />
              {line.label}
            </span>
            <span className={styles.ticketCount}>{line.quantity}매</span>
          </div>
        ))}
      </div>

      <div className={styles.actions}>
        <button type="button" className={styles.eventButton} onClick={() => onViewEvent?.(order.eventId)}>
          박람회 보기
          <ExternalLink size={14} />
        </button>
        {order.status !== "CANCELLED" && order.status !== "PENDING" && order.totalAmount > 0 && (
          <button
            type="button"
            className={styles.paymentButton}
            onClick={() => onViewPayment?.(order.orderId, event?.startDate)}
          >
            결제 정보 확인하기
            <ChevronRight size={14} />
          </button>
        )}
      </div>
    </article>
  );
}
