"use client";

import { useSearchParams } from "next/navigation";
import { AttendeeListContent } from "@/features/reservation/components/AttendeeListContent";

export default function ManagerReservationsPage() {
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId") ?? "1";
  return <AttendeeListContent eventId={eventId} />;
}
