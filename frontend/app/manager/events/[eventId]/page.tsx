"use client";

import { use } from "react";
import { EventDetail } from "@/features/event/components/EventDetail/EventDetail";

export default function ManagerEventDetailPage({ params }: { params: Promise<{ eventId: string }> }) {
  const { eventId } = use(params);
  return <EventDetail eventId={eventId} mode="manager" />;
}