import { EventSelectContent } from "@/features/reservation/components/EventSelectContent";

export default function ReservationsSummaryPage() {
  return (
    <EventSelectContent
        basePath="/admin/reservations-summary" title={""} subtitle={""}    showSearch />
  );
}
