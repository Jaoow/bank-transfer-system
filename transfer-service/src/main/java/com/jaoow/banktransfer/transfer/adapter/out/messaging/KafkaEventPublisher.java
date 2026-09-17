package com.jaoow.banktransfer.transfer.adapter.out.messaging;

import tools.jackson.databind.ObjectMapper;
import com.jaoow.banktransfer.transfer.domain.port.out.EventPublisher;
import com.jaoow.banktransfer.transfer.adapter.in.web.EventStreamController;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventStreamController eventStreamController;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, EventStreamController eventStreamController) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventStreamController = eventStreamController;
    }

    @Override
    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            
            // Broadcast via SSE para o frontend
            eventStreamController.broadcastEvent(topic, key, payload);
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao publicar evento no tópico " + topic, ex);
        }
    }
}