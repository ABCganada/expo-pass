"use client";

import { use } from "react";
import { AdminEventDetail } from "@/features/event/components/AdminEventDetail/AdminEventDetail";

export default function AdminEventDetailPage({ params }: { params: Promise<{ eventId: string }> }) {
  const { eventId } = use(params);
  return <AdminEventDetail eventId={eventId} />;
}