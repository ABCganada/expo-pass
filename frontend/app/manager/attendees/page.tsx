"use client";

import { useSearchParams } from "next/navigation";
import { AttendeeListContent } from "@/features/reservation/components/AttendeeListContent";

export default function AttendeesPage() {
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId") ?? "1";
  return <AttendeeListContent eventId={eventId} />;
}
