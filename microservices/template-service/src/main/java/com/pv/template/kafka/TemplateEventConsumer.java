package com.pv.template.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TemplateEventConsumer {

    @KafkaListener(
        topics = "${kafka.topic.inbound-events:template.inbound}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            log.info("Received Kafka event: topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
            // TODO: implement domain logic
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing Kafka record key={}: {}", record.key(), ex.getMessage(), ex);
        }
    }
}
