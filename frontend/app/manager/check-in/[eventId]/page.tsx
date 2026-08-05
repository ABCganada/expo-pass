"use client";

import { use } from "react";
import { CheckinContent } from "@/features/reservation/components/CheckinContent";

export default function ManagerCheckInDetailPage({
  params,
}: {
  params: Promise<{ eventId: string }>;
}) {
  const { eventId } = use(params);
  return <CheckinContent eventId={eventId} />;
}
