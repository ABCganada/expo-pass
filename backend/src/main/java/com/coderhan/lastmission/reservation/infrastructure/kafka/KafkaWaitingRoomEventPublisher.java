package com.coderhan.lastmission.reservation.infrastructure.kafka;

import com.coderhan.lastmission.reservation.application.WaitingRoomEventPublisher;
import com.coderhan.lastmission.shared.realtime.AfterCommitExecutor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class KafkaWaitingRoomEventPublisher implements WaitingRoomEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate; // 기존 KafkaTestController와 같은 빈 재사용
    private final ObjectMapper objectMapper;
    private final AfterCommitExecutor afterCommitExecutor;     // 커밋 이후에만 발행되게 보장

    @Value("${lastmission.kafka.topic}")
    private String topic; // 새 토픽 안 만들고 기존 토픽 재사용

    @Override
    public void publishJoined(long ticketNo, long eventId, long userId) {
        WaitingRoomJoinedEvent event = new WaitingRoomJoinedEvent(ticketNo, eventId, userId);
        // 지금 바로 안 보내고, DB 트랜잭션이 커밋된 뒤에 보내도록 예약
        afterCommitExecutor.execute(() -> send(event));
    }

    private void send(WaitingRoomJoinedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = String.valueOf(event.eventId()); // 같은 행사는 항상 같은 파티션으로 감 → 순서 보장
            kafkaTemplate.send(topic, key, payload);        // 기존 2-인자 대신 3-인자(topic, key, value) 사용
        } catch (JacksonException e) {
            log.error("대기열 입장 이벤트 직렬화 실패: {}", event, e);
        }
    }
}
