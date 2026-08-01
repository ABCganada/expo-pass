package com.coderhan.lastmission.reservation.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WaitingRoomQueue {
    long register(long userId, long eventId);
    long getRank(long userId, long eventId);
    Map<Long, Long> getAllRanks(long eventId);
    List<Long> allowEntry(long eventId, int count);
    void consumeTicket(long userId, long eventId);
    void leave(long userId, long eventId);
    Set<Long> getActiveEventIds();
    void clearAll();
}
