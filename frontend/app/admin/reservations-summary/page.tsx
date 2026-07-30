import { EventSelectContent } from "@/features/reservation/components/EventSelectContent";

export default function ReservationsSummaryPage() {
  return (
    <EventSelectContent
      basePath="/admin/reservations-summary"
      title="행사별 예약 현황"
      subtitle="예약 현황을 확인할 행사를 선택하세요"
    />
  );
}
