package com.coderhan.lastmission.chat.application;

import com.coderhan.lastmission.chat.domain.ChatMessage;

/**
 * 저장된 메시지를 그 방을 보고 있는 접속자에게 밀어준다.
 *
 * <p>구현은 인프라(WebSocket)에 있다. 서비스는 전달 수단을 모른다 —
 * 나중에 파드를 늘리려면 이 자리에 Kafka 팬아웃 구현을 끼우면 된다.</p>
 */
public interface ChatBroadcaster {
    void broadcast(ChatMessage message);
}
