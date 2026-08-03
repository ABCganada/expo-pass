"use client";

import { use } from "react";
import { AdminEventDetail } from "@/features/event/components/AdminEventDetail/AdminEventDetail";

export default function ManagerEventDetailPage({ params }: { params: Promise<{ eventId: string }> }) {
  const { eventId } = use(params);
  return <AdminEventDetail eventId={eventId} basePath="/manager/events" />;
}