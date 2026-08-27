/**
 * 여러 모듈이 함께 쓰는 STOMP over WebSocket 연결.
 *
 * <p>연결은 사용자당 하나이고, 그 위에 채팅·공지·접속 현황이 함께 흐른다.
 * 어떤 갈래를 받을지는 구독 주소(destination)로 나눈다.</p>
 */
@org.springframework.modulith.NamedInterface("realtime")
package com.coderhan.lastmission.shared.realtime;
