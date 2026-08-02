package com.coderhan.lastmission.reservation.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WaitingRoomQueue {
    void register(long userId, long eventId);
    long getRank(long userId, long eventId);
    Map<Long, Long> getAllRanks(long eventId);
    List<Long> allowEntry(long eventId, int count);
    /** 아직 쓰지 않은 입장 티켓이 남아 있는지 확인한다(소비하지 않는다). */
    boolean hasTicket(long userId, long eventId);
    void consumeTicket(long userId, long eventId);
    void leave(long userId, long eventId);
    Set<Long> getActiveEventIds();
}
