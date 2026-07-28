package com.coderhan.lastmission.reservation.application;

/** 대기열 입장 이벤트 발행. 실제로 카프카를 쓰는지는 구현체(infrastructure)만 안다. */
public interface WaitingRoomEventPublisher {
    void publishJoined(long ticketNo, long eventId, long userId);
}
