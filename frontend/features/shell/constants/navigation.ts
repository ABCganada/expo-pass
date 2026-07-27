export interface NavigationItem {
  id: "exhibitions" | "reservations" | "qr-ticket";
  label: string;
  path: string;
}

export const USER_MENU_ITEMS: NavigationItem[] = [
  { id: "exhibitions", label: "박람회 예약하기", path: "/exhibitions" },
  { id: "reservations", label: "내 예약 내역", path: "/reservations" },
  { id: "qr-ticket", label: "QR 티켓", path: "/tickets/qr" },
];
