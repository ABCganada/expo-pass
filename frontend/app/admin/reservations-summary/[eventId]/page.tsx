"use client";

import { use } from "react";
import { ReservationSummaryContent } from "@/features/reservation/components/ReservationSummaryContent";

export default function ReservationsSummaryDetailPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = use(params);
  return <ReservationSummaryContent eventId={eventId} />;
}
