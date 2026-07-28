package com.coderhan.lastmission.reservation.infrastructure.kafka;

import java.time.Clock;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.reservation.application.WaitingRoomRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class WaitingRoomAdmissionConsumer {
    private static final long ADMISSION_DELAY_MS = 200;

    private final WaitingRoomRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @KafkaListener(
            topics = "${lastmission.kafka.topic}",
            groupId = "${lastmission.kafka.consumer-group}",
            autoStartup = "${lastmission.kafka.enabled:false}"
    )
    public void listen(String message) {
        WaitingRoomJoinedEvent event = tryParse(message);
        if (event == null) return;

        boolean admitted = repository.admit(event.ticketNo(), OffsetDateTime.now(clock));
        if (admitted) {
            log.info("대기열 허가: ticketNo={}, eventId={}", event.ticketNo(), event.eventId());
        }
        throttle();
    }

    private WaitingRoomJoinedEvent tryParse(String message) {
        try {
            WaitingRoomJoinedEvent event = objectMapper.readValue(message, WaitingRoomJoinedEvent.class);
            return WaitingRoomJoinedEvent.TYPE.equals(event.type()) ? event : null;
        } catch (JacksonException e) {
            return null;
        }
    }
    //초당 5개....
    private void throttle() {
        try {
            Thread.sleep(ADMISSION_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
