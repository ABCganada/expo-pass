import type { EventTicket, CreateTicketPayload, UpdateTicketPayload } from "../../types/eventManagementDetail";

export interface TicketDraft {
  name: string;
  price: string;
  quantityTotal: string;
  maxPurchasePerUser: string;
  saleStartAt: string;
  saleEndAt: string;
}

export const EMPTY_DRAFT: TicketDraft = {
  name: "",
  price: "",
  quantityTotal: "",
  maxPurchasePerUser: "",
  saleStartAt: "",
  saleEndAt: "",
};

export function toDatetimeLocal(iso: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function fromDatetimeLocal(value: string): string | null {
  return value ? new Date(value).toISOString() : null;
}

export function formatSalePeriod(startAt: string | null, endAt: string | null): string {
  if (!startAt || !endAt) return "미정";
  const format = (iso: string) => new Date(iso).toLocaleString("ko-KR", { dateStyle: "short", timeStyle: "short" });
  return `${format(startAt)}  →  ${format(endAt)}`;
}

export type TicketStatusTone = "upcoming" | "ongoing" | "ended" | "soldout";

export function ticketStatus(ticket: EventTicket): { label: string; tone: TicketStatusTone } {
  const now = new Date();
  if (ticket.quantityRemaining <= 0) return { label: "매진", tone: "soldout" };
  if (ticket.saleStartAt && now < new Date(ticket.saleStartAt)) return { label: "판매예정", tone: "upcoming" };
  if (ticket.saleEndAt && now > new Date(ticket.saleEndAt)) return { label: "판매종료", tone: "ended" };
  return { label: "판매중", tone: "ongoing" };
}

export function draftFromTicket(ticket: EventTicket): TicketDraft {
  return {
    name: ticket.name,
    price: String(ticket.price),
    quantityTotal: String(ticket.quantityTotal),
    maxPurchasePerUser: String(ticket.maxPurchasePerUser),
    saleStartAt: toDatetimeLocal(ticket.saleStartAt),
    saleEndAt: toDatetimeLocal(ticket.saleEndAt),
  };
}

export function draftToCreatePayload(draft: TicketDraft): CreateTicketPayload {
  return {
    name: draft.name,
    price: Number(draft.price),
    quantityTotal: Number(draft.quantityTotal),
    maxPurchasePerUser: Number(draft.maxPurchasePerUser),
    saleStartAt: fromDatetimeLocal(draft.saleStartAt),
    saleEndAt: fromDatetimeLocal(draft.saleEndAt),
  };
}

export function draftToUpdatePayload(draft: TicketDraft): UpdateTicketPayload {
  return {
    name: draft.name,
    price: Number(draft.price),
    quantityTotal: Number(draft.quantityTotal),
    maxPurchasePerUser: Number(draft.maxPurchasePerUser),
    saleStartAt: fromDatetimeLocal(draft.saleStartAt),
    saleEndAt: fromDatetimeLocal(draft.saleEndAt),
  };
}