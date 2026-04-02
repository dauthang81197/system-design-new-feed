package com.pv.template.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.template-events:template.events}")
    private String topic;

    public void publishEvent(String eventType, String templateId, String actorId) {
        Map<String, Object> payload = Map.of(
            "eventType", eventType,
            "templateId", templateId,
            "actorId", actorId,
            "timestamp", Instant.now().toString()
        );
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, templateId, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send Kafka event [{}] for templateId={}: {}", eventType, templateId, ex.getMessage());
            } else {
                log.debug("Kafka event [{}] sent: partition={} offset={}",
                    eventType,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
