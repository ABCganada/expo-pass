package com.coderhan.lastmission.reservation.infrastructure.kafka;

/**
 * 대기열 입장 이벤트. 기존 토픽(lastmission.main.events)을 다른 목적(카프카 테스트)과
 * 공유하므로, {@code type} 필드로 이 메시지가 뭔지 구분한다. -> 추후 대천님과 상의 후에 토픽을 추가할 수도 있음.
 */
public record WaitingRoomJoinedEvent(String type, long ticketNo, long eventId, long userId) {
    public static final String TYPE = "WAITING_ROOM_JOINED";

    public WaitingRoomJoinedEvent(long ticketNo, long eventId, long userId) {
        this(TYPE, ticketNo, eventId, userId);
    }
}
