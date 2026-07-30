"use client";

import { useSearchParams } from "next/navigation";
import { ReservationSummaryContent } from "@/features/reservation/components/ReservationSummaryContent";

export default function ReservationsSummaryPage() {
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId") ?? "1";
  return <ReservationSummaryContent eventId={eventId} />;
}
