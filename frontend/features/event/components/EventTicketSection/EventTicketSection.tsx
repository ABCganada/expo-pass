"use client";

import { TicketPurchaseFlow } from "@/features/reservation/components/TicketPurchaseFlow";
import type { TicketTypeOption } from "@/features/reservation/components/TicketSelectionPanel";
import type { EventTicket } from "../../types/event";
import styles from "./EventTicketSection.module.css";

interface EventTicketSectionProps {
  eventId: string;
  tickets: EventTicket[];
}

function formatPrice(price: number): string {
  return price === 0 ? "무료" : `${price.toLocaleString("ko-KR")}원`;
}

export function EventTicketSection({ eventId, tickets }: EventTicketSectionProps) {
  const ticketTypes: TicketTypeOption[] = tickets.map((ticket) => ({
    ticketId: ticket.id,
    name: ticket.name,
    price: ticket.price,
    maxPerUser: ticket.maxPurchasePerUser,
  }));

  return (
    <div className={styles.section}>
      <p className={styles.title}>티켓 선택</p>

      {tickets.length === 0 ? (
        <p className={styles.empty}>등록된 티켓이 없습니다.</p>
      ) : (
        <ul className={styles.list}>
          {tickets.map((ticket) => (
            <li key={ticket.id} className={styles.row}>
              <span className={styles.name}>{ticket.name}</span>
              <span className={styles.price}>{formatPrice(ticket.price)}</span>
            </li>
          ))}
        </ul>
      )}

      <TicketPurchaseFlow eventId={eventId} ticketTypes={ticketTypes} />
    </div>
  );
}