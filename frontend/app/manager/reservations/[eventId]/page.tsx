"use client";

import { use } from "react";
import { AttendeeListContent } from "@/features/reservation/components/AttendeeListContent";

export default function ManagerReservationsDetailPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = use(params);
  return <AttendeeListContent eventId={eventId} />;
}
