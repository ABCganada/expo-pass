package com.coderhan.lastmission.event.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class EventViewServiceTest {
    private static final long EVENT_ID = 1L;
    private static final long USER_ID = 300L;

    @Mock EventService eventService;
    @Mock RedissonClient redissonClient;
    @Mock RSet<Long> viewers;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks EventViewService service;

    @Test
    void increaseIfNew_처음_보는_유저면_조회수_증가() {
        when(redissonClient.<Long>getSet(anyString())).thenReturn(viewers);
        when(viewers.add(USER_ID)).thenReturn(true);

        service.increaseIfNew(EVENT_ID, USER_ID);

        verify(viewers).expire(Duration.ofHours(24));
        verify(eventService).increaseViewCount(EVENT_ID);
    }

    @Test
    void increaseIfNew_이미_본_유저면_스킵() {
        when(redissonClient.<Long>getSet(anyString())).thenReturn(viewers);
        when(viewers.add(USER_ID)).thenReturn(false);

        service.increaseIfNew(EVENT_ID, USER_ID);

        verify(eventService, never()).increaseViewCount(EVENT_ID);
    }

    @Test
    void increaseIfNew_DB_반영_실패하면_dedup_마킹을_되돌리기() {
        when(redissonClient.<Long>getSet(anyString())).thenReturn(viewers);
        when(viewers.add(USER_ID)).thenReturn(true);
        doThrow(new RuntimeException("db down")).when(eventService).increaseViewCount(EVENT_ID);

        service.increaseIfNew(EVENT_ID, USER_ID);

        verify(viewers).remove(USER_ID);
    }
}