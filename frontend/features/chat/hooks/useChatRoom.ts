"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "@/features/store/store";
import { queryErrorMessage } from "@/features/store/api/queryError";
import {
  chatApi,
  mergeChatMessages,
  useGetChatMessagesQuery,
  useLazyGetChatMessagesQuery,
  useMarkChatRoomReadMutation,
  useSendChatMessageMutation,
} from "../api/chatApi";
import { isChatMessage } from "../services/chatService";
import { useSocket } from "../../socket/useSocket";

export type ConnectionState = "connecting" | "live" | "offline";

export function useChatRoom(roomId: string | null) {
  const dispatch = useDispatch<AppDispatch>();
  const [localError, setLocalError] = useState<string | null>(null);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [renderedRoomId, setRenderedRoomId] = useState(roomId);
  const { state: connection, error: socketError, subscribe } = useSocket();
  const previousConnectionRef = useRef<ConnectionState>(connection);

  if (roomId !== renderedRoomId) {
    setRenderedRoomId(roomId);
    setHasMore(true);
    setLocalError(null);
  }

  const queryArgument = { roomId: roomId ?? "" };
  const messagesQuery = useGetChatMessagesQuery(queryArgument, {
    skip: roomId === null,
    refetchOnFocus: true,
    refetchOnReconnect: true,
  });
  const [loadMessages] = useLazyGetChatMessagesQuery();
  const [sendMessage] = useSendChatMessageMutation();
  const [markRoomRead] = useMarkChatRoomReadMutation();
  const messages = useMemo(
    () => messagesQuery.data ?? [],
    [messagesQuery.data],
  );
  const refetchMessages = messagesQuery.refetch;

  const markRead = useCallback(async () => {
    if (!roomId) return;
    try {
      await markRoomRead(roomId).unwrap();
    } catch (reason: unknown) {
      setLocalError(queryErrorMessage(
        reason,
        "읽음 상태를 저장하지 못했습니다.",
      ));
    }
  }, [markRoomRead, roomId]);

  useEffect(() => {
    if (!roomId) return;
    const unsubscribe = subscribe(`/topic/chat.${roomId}`, (body) => {
      if (!isChatMessage(body)) return;
      dispatch(chatApi.util.updateQueryData(
        "getChatMessages",
        { roomId },
        (draft) => mergeChatMessages(draft, [body]),
      ));
      void markRead();
    });
    return unsubscribe;
  }, [dispatch, markRead, roomId, subscribe]);

  useEffect(() => {
    if (roomId && messagesQuery.isSuccess) {
      void markRoomRead(roomId);
    }
  }, [markRoomRead, messagesQuery.isSuccess, roomId]);

  useEffect(() => {
    const previous = previousConnectionRef.current;
    previousConnectionRef.current = connection;
    if (roomId && previous === "offline" && connection === "live") {
      void refetchMessages();
    }
  }, [connection, refetchMessages, roomId]);

  const loadOlder = useCallback(async () => {
    if (!roomId || isLoadingMore || !hasMore || messages.length === 0) return;
    setIsLoadingMore(true);
    const currentIds = new Set(messages.map((message) => message.id));
    try {
      const result = await loadMessages({
        roomId,
        before: messages[0].id,
      }, false).unwrap();
      if (!result.some((message) => !currentIds.has(message.id))) {
        setHasMore(false);
      }
      setLocalError(null);
    } catch (reason: unknown) {
      setLocalError(queryErrorMessage(
        reason,
        "이전 대화를 불러오지 못했습니다.",
      ));
    } finally {
      setIsLoadingMore(false);
    }
  }, [hasMore, isLoadingMore, loadMessages, messages, roomId]);

  const send = useCallback(async (content: string) => {
    if (!roomId) return;
    const trimmed = content.trim();
    if (!trimmed) return;
    try {
      const saved = await sendMessage({ roomId, content: trimmed }).unwrap();
      dispatch(chatApi.util.updateQueryData(
        "getChatMessages",
        { roomId },
        (draft) => mergeChatMessages(draft, [saved]),
      ));
      setLocalError(null);
    } catch (reason: unknown) {
      setLocalError(queryErrorMessage(reason, "메시지를 보내지 못했습니다."));
    }
  }, [dispatch, roomId, sendMessage]);

  return {
    messages,
    connection,
    error: localError ??
      (messagesQuery.error
        ? queryErrorMessage(messagesQuery.error, "대화를 불러오지 못했습니다.")
        : socketError),
    send,
    loadOlder,
    isLoadingMore,
    hasMore,
  };
}
