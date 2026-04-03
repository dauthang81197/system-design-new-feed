package com.pv.posts.kafka;

import com.pv.posts.dto.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventProducer {

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.post-created}")
    private String postCreatedTopic;

    public void publishPostCreated(PostCreatedEvent event) {
        String key = event.getPostId().toString();

        CompletableFuture<SendResult<String, PostCreatedEvent>> future =
                kafkaTemplate.send(postCreatedTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Failed to publish post.created event for postId={}: {}",
                        event.getPostId(), ex.getMessage(), ex);
            } else {
                log.info("[Kafka] Published post.created event for postId={} to topic={} partition={} offset={}",
                        event.getPostId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}

