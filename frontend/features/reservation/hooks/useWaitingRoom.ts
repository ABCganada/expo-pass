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

    console.log("[waiting-room] EventSource 생성", { eventId });

    source.addEventListener("rank", (event) => {
      console.log("[waiting-room] rank 이벤트 수신", (event as MessageEvent).data);
      setError(null);
      setRank(Number((event as MessageEvent).data));
    });

    source.addEventListener("admitted", () => {
      console.log("[waiting-room] admitted 이벤트 수신, onAdmitted 호출 시도");
      source.close();
      onAdmittedRef.current();
      console.log("[waiting-room] onAdmitted 호출 완료");
    });

    source.onerror = (e) => {
      console.log("[waiting-room] onerror 발생", e, "readyState=", source.readyState);
      setError("대기열 연결이 불안정합니다. 재연결을 시도합니다...");
    };

    return () => {
      source.close();
    };
  }, [eventId, enabled]);

  return { rank, error };
}
