package com.coderhan.lastmission.reservation.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class WaitingRoomPingSchedulerTest {

    @Mock WaitingRoomEmitterManager emitterManager;

    @InjectMocks WaitingRoomPingScheduler scheduler;

    @Test
    void completesBrokenEmitterAndStillPingsTheRest() throws IOException {
        SseEmitter broken = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        when(emitterManager.getAll()).thenReturn(List.of(broken, healthy));

        scheduler.ping();

        verify(broken).complete();
        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void continuesPingingOthersWhenCompleteThrowsIllegalStateException() throws IOException {
        SseEmitter broken = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        doThrow(new IllegalStateException("already errored")).when(broken).complete();
        when(emitterManager.getAll()).thenReturn(List.of(broken, healthy));

        scheduler.ping();

        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void continuesPingingOthersWhenSendItselfThrowsIllegalStateException() throws IOException {
        SseEmitter broken = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IllegalStateException("already completed")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        when(emitterManager.getAll()).thenReturn(List.of(broken, healthy));

        scheduler.ping();

        verify(broken).complete();
        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
    }
}
