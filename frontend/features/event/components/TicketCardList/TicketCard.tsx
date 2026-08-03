import type { AdminEventTicket } from "../../types/adminEventDetail";
import { formatSalePeriod, ticketStatus } from "./ticketUtils";
import styles from "./TicketCard.module.css";

interface TicketCardProps {
  ticket: AdminEventTicket;
  disabled: boolean;
  onEdit: () => void;
  onDelete: () => void;
}

export function TicketCard({ ticket, disabled, onEdit, onDelete }: TicketCardProps) {
  const status = ticketStatus(ticket);

  return (
    <div className={styles.card}>
      <div className={styles.cardHeaderRow}>
        <span className={styles.titleText}>{ticket.name}</span>
        <div className={styles.headerRight}>
          <span className={styles.statusBadge} data-tone={status.tone}>
            {status.label}
          </span>
          <div className={styles.inlineActions}>
            <button type="button" className={styles.editButton} disabled={disabled} onClick={onEdit}>
              수정
            </button>
            <button type="button" className={styles.deleteButton} disabled={disabled} onClick={onDelete}>
              삭제
            </button>
          </div>
        </div>
      </div>
      <div className={styles.priceText}>{ticket.price.toLocaleString("ko-KR")}원</div>
      <div className={styles.divider} />
      <div className={styles.row}>
        <span className={styles.label}>총수량</span>
        <span className={styles.value}>{ticket.quantityTotal}</span>
      </div>
      <div className={styles.row}>
        <span className={styles.label}>인당 최대</span>
        <span className={styles.value}>{ticket.maxPurchasePerUser}</span>
      </div>
      <div className={styles.row}>
        <span className={styles.label}>판매기간</span>
        <span className={styles.value}>{formatSalePeriod(ticket.saleStartAt, ticket.saleEndAt)}</span>
      </div>
    </div>
  );
}