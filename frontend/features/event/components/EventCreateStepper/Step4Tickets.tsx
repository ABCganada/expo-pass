"use client";

import { TicketCardList } from "../TicketCardList/TicketCardList";
import type { EventTicket } from "../../types/eventManagementDetail";
import styles from "./EventCreateStepper.module.css";

interface Step4TicketsProps {
  eventId: string;
  tickets: EventTicket[];
  onBack: () => void;
  onComplete: () => void;
}

export function Step4Tickets({ eventId, tickets, onBack, onComplete }: Step4TicketsProps) {
  return (
    <div className={styles.pageCenter}>
      <div className={styles.stepBody}>
        <TicketCardList eventId={eventId} tickets={tickets} mode="manager" />
        <div className={styles.stepActions}>
          <button type="button" className={styles.backButton} onClick={onBack}>
            이전
          </button>
          <button type="button" className={styles.nextButton} onClick={onComplete}>
            완료
          </button>
        </div>
      </div>
    </div>
  );
}