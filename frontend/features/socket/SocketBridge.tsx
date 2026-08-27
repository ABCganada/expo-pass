"use client";

import { useEffect } from "react";
import { useGetCurrentUserQuery } from "@/features/auth/api/authApi";
import { useAppDispatch } from "@/features/store/hooks";
import { socketManager } from "./SocketManager";

/**
 * 인증된 사용자에게만 전역 STOMP 연결을 연다.
 *
 * RootLayout 아래에 한 번만 존재하므로 일반/관리자 화면을 오가더라도 같은 연결 관리자를
 * 사용한다. 실제 Client 객체는 Redux가 아니라 SocketManager 내부에만 머문다.
 */
export function SocketBridge() {
  const dispatch = useAppDispatch();
  const { data: user } = useGetCurrentUserQuery();
  const authenticated = user !== null && user !== undefined;

  useEffect(() => {
    if (!authenticated) {
      socketManager.disconnect(dispatch);
      return;
    }
    socketManager.connect(dispatch);
    return () => socketManager.disconnect(dispatch);
  }, [authenticated, dispatch]);

  return null;
}
