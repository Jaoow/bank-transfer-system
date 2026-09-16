package com.jaoow.banktransfer.transfer.adapter.out.messaging;

import tools.jackson.databind.ObjectMapper;
import com.jaoow.banktransfer.transfer.domain.port.out.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao publicar evento no tópico " + topic, ex);
        }
    }
}