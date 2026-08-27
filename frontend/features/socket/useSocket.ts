"use client";

import { useAppSelector } from "@/features/store/hooks";
import { socketManager } from "./SocketManager";
import type { SocketState } from "./store/socketSlice";

interface SocketValue {
  state: SocketState;
  error: string | null;
  connectedAt: string | null;
  subscribe: (
    destination: string,
    onMessage: (body: unknown) => void,
  ) => () => void;
}

export function useSocket(): SocketValue {
  const socket = useAppSelector((state) => state.socket);
  return {
    ...socket,
    subscribe: socketManager.subscribe,
  };
}
