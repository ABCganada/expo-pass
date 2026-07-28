package com.coderhan.lastmission.kafka;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 테스트 토픽 수신자. 로컬에는 카프카가 없으므로 lastmission.kafka.enabled로 리스너를 켜고 끈다.
 */
@Component
public class KafkaTestReceiver {

    private static final int MAX_KEPT = 20;

    private final ConcurrentLinkedDeque<ReceivedMessage> messages = new ConcurrentLinkedDeque<>();

    public record ReceivedMessage(String message, LocalDateTime receivedAt) {
    }

    @KafkaListener(
            topics = "${lastmission.kafka.topic}",
            groupId = "lastmission-main-dev-kafka-test-receiver",
            autoStartup = "${lastmission.kafka.enabled:false}"
    )
    public void listen(String message) {
        messages.addFirst(new ReceivedMessage(message, LocalDateTime.now()));
        while (messages.size() > MAX_KEPT) {
            messages.removeLast();
        }
    }

    public List<ReceivedMessage> recent() {
        return List.copyOf(messages);
    }
}
