"use client";

import { useRef, useState } from "react";
import { ChevronLeft, ChevronRight, QrCode as QrCodeIcon } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { useGetMyQrTicketsQuery } from "../../api/reservationApi";
import { useGetEventForDisplayQuery } from "../../api/eventLookupApi";
import styles from "./QrTicketContent.module.css";

interface EventTabButtonProps {
  eventId: string;
  active: boolean;
  onClick: () => void;
}

// 탭 하나가 자기 eventId로 행사 이름을 직접 조회한다 — 같은 eventId를 쓰는 탭이 여러 개일 순 없지만,
// 다른 화면(내 예약 내역 등)과 같은 eventId를 조회하면 RTK Query 캐시를 그대로 공유한다.
function EventTabButton({ eventId, active, onClick }: EventTabButtonProps) {
  const { data: event } = useGetEventForDisplayQuery(eventId);
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      className={styles.tab}
      data-active={active}
      onClick={onClick}
    >
      {event?.title ?? `행사 #${eventId}`}
    </button>
  );
}

export function QrTicketContent() {
  const { data: tickets = [], isLoading } = useGetMyQrTicketsQuery();
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [ticketIndex, setTicketIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);

  if (isLoading) {
    return (
      <div className={styles.page}>
        <p className={styles.stateText}>불러오는 중...</p>
      </div>
    );
  }

  if (tickets.length === 0) {
    return (
      <div className={styles.page}>
        <div className={styles.emptyState}>
          <div className={styles.emptyIconWrap}>
            <QrCodeIcon size={28} />
          </div>
          <div className={styles.emptyBody}>
            <p className={styles.emptyTitle}>표시할 QR 티켓이 없습니다</p>
            <p className={styles.emptyDescription}>예약이 확정되면 여기서 입장용 QR을 확인할 수 있어요.</p>
          </div>
        </div>
      </div>
    );
  }

  const eventIds = Array.from(new Set(tickets.map((ticket) => ticket.eventId)));
  const activeEventId = selectedEventId ?? eventIds[0];
  const groupTickets = tickets.filter((ticket) => ticket.eventId === activeEventId);
  const safeIndex = Math.min(ticketIndex, groupTickets.length - 1);
  const ticket = groupTickets[safeIndex];

  function selectEvent(eventId: string) {
    setSelectedEventId(eventId);
    setTicketIndex(0);
  }

  function goTo(delta: number) {
    const next = safeIndex + delta;
    if (next < 0 || next >= groupTickets.length) return;
    setTicketIndex(next);
  }

  function handleTouchStart(e: React.TouchEvent) {
    touchStartX.current = e.touches[0].clientX;
  }

  function handleTouchEnd(e: React.TouchEvent) {
    if (touchStartX.current === null) return;
    const deltaX = e.changedTouches[0].clientX - touchStartX.current;
    touchStartX.current = null;
    if (Math.abs(deltaX) < 40) return;
    goTo(deltaX < 0 ? 1 : -1);
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>QR 티켓</h1>
        <p className={styles.subtitle}>입장 시 이 화면을 스캐너에 보여주세요.</p>
      </header>

      {eventIds.length > 1 && (
        <div className={styles.tabs} role="tablist">
          {eventIds.map((eventId) => (
            <EventTabButton
              key={eventId}
              eventId={eventId}
              active={eventId === activeEventId}
              onClick={() => selectEvent(eventId)}
            />
          ))}
        </div>
      )}

      <div className={styles.card} onTouchStart={handleTouchStart} onTouchEnd={handleTouchEnd}>
        <button
          type="button"
          aria-label="이전 티켓"
          className={styles.navButton}
          onClick={() => goTo(-1)}
          disabled={safeIndex === 0}
        >
          <ChevronLeft size={20} />
        </button>

        <div className={styles.qrArea} data-checked-in={ticket.checkedInAt !== null}>
          <QRCodeSVG value={ticket.qrCodeHash} size={240} />
          {ticket.checkedInAt !== null && <span className={styles.checkedBadge}>입장완료</span>}
        </div>

        <button
          type="button"
          aria-label="다음 티켓"
          className={styles.navButton}
          onClick={() => goTo(1)}
          disabled={safeIndex === groupTickets.length - 1}
        >
          <ChevronRight size={20} />
        </button>
      </div>

      <p className={styles.counter}>
        {safeIndex + 1} / {groupTickets.length}
      </p>
      <p className={styles.orderId}>{ticket.orderId}</p>
    </div>
  );
}
