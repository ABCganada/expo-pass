import { EventSelectContent } from "@/features/reservation/components/EventSelectContent";

export default function ManagerReservationsPage() {
  return (
    <EventSelectContent
      basePath="/manager/reservations"
      title="예약자 명단 관리"
      subtitle="예약자 명단을 확인할 행사를 선택하세요"
      showPhaseTabs
      showSearch
    />
  );
}
