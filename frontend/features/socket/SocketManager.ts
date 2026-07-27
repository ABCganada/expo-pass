import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import type { AppDispatch } from "@/features/store/store";
import {
  socketConnected,
  socketConnecting,
  socketDisconnected,
  socketFailed,
} from "./store/socketSlice";

type MessageHandler = (body: unknown) => void;

function parseBody(frame: IMessage): unknown {
  try {
    return JSON.parse(frame.body);
  } catch {
    return frame.body;
  }
}

function socketUrl(): string {
  const base = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");
  if (base) return `${base.replace(/^http/, "ws")}/ws`;
  return `${window.location.origin.replace(/^http/, "ws")}/ws`;
}

class SocketManager {
  private client: Client | null = null;
  private readonly pending = new Map<string, Set<MessageHandler>>();
  private readonly active = new Map<string, StompSubscription>();

  connect(dispatch: AppDispatch): void {
    if (this.client?.active) return;

    dispatch(socketConnecting());
    const client = new Client({
      brokerURL: socketUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 25000,
      heartbeatOutgoing: 25000,
    });

    client.onConnect = () => {
      dispatch(socketConnected(new Date().toISOString()));
      this.active.clear();
      this.pending.forEach((_handlers, destination) => {
        this.bind(destination);
      });
    };
    client.onWebSocketClose = () => {
      this.active.clear();
      dispatch(socketDisconnected());
    };
    client.onStompError = (frame) => {
      dispatch(socketFailed(
        frame.headers["message"] ?? "실시간 연결이 거절되었습니다.",
      ));
    };

    this.client = client;
    client.activate();
  }

  disconnect(dispatch: AppDispatch): void {
    const client = this.client;
    this.client = null;
    this.active.clear();
    if (client) void client.deactivate();
    dispatch(socketDisconnected());
  }

  readonly subscribe = (
    destination: string,
    onMessage: MessageHandler,
  ): (() => void) => {
    const handlers = this.pending.get(destination) ?? new Set();
    handlers.add(onMessage);
    this.pending.set(destination, handlers);
    this.bind(destination);

    return () => {
      const current = this.pending.get(destination);
      current?.delete(onMessage);
      if (current && current.size === 0) {
        this.pending.delete(destination);
        this.active.get(destination)?.unsubscribe();
        this.active.delete(destination);
      }
    };
  };

  private bind(destination: string): void {
    const client = this.client;
    if (!client?.connected || this.active.has(destination)) return;
    this.active.set(destination, client.subscribe(destination, (frame) => {
      this.pending.get(destination)
        ?.forEach((handler) => handler(parseBody(frame)));
    }));
  }
}

export const socketManager = new SocketManager();
