"use client";

import { useEffect, useRef, useState } from "react";

const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

interface UseWaitingRoomResult {
  rank: number | null;
  error: string | null;
}

export function useWaitingRoom(
  eventId: string,
  enabled: boolean,
  onAdmitted: () => void,
): UseWaitingRoomResult {
  const [rank, setRank] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const onAdmittedRef = useRef(onAdmitted);
  useEffect(() => {
    onAdmittedRef.current = onAdmitted;
  });

  useEffect(() => {
    if (!enabled) return;

    const source = new EventSource(
      `${API_BASE_URL}/api/v1/reservations/waiting-room/${eventId}/stream`,
      { withCredentials: true },
    );

    source.addEventListener("rank", (event) => {
      setError(null);
      setRank(Number((event as MessageEvent).data));
    });

    source.addEventListener("admitted", () => {
      source.close();
      onAdmittedRef.current();
    });

    source.onerror = () => {
      setError("대기열 연결이 불안정합니다. 재연결을 시도합니다...");
    };

    return () => {
      source.close();
    };
  }, [eventId, enabled]);

  return { rank, error };
}
