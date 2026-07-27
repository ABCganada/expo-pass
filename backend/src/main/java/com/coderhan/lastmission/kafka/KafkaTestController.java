package com.coderhan.lastmission.kafka;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kafka 연결 검증용 테스트 엔드포인트. 보내기(send) 후 수신 목록(received)으로 왕복을 확인한다.
 */
@RestController
@RequestMapping("/api/kafka/test")
@RequiredArgsConstructor
public class KafkaTestController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTestReceiver receiver;

    @Value("${lastmission.kafka.topic}")
    private String topic;

    public record SendRequest(String message) {
    }

    public record SendResponse(String topic, String message, LocalDateTime sentAt) {
    }

    @PostMapping("/send")
    public SendResponse send(@RequestBody SendRequest request) {
        String message = request.message() != null ? request.message() : "ping";
        kafkaTemplate.send(topic, message);
        return new SendResponse(topic, message, LocalDateTime.now());
    }

    @GetMapping("/received")
    public List<KafkaTestReceiver.ReceivedMessage> received() {
        return receiver.recent();
    }
}
