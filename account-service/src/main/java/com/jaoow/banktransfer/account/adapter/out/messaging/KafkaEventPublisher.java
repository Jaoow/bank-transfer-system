package com.jaoow.banktransfer.account.adapter.out.messaging;

import com.jaoow.banktransfer.account.adapter.out.persistence.OutboxEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.OutboxEventJpaRepository;
import com.jaoow.banktransfer.account.domain.port.out.EventPublisher;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(OutboxEventJpaRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEventJpaEntity(topic, key, payload));
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao salvar evento no outbox para o tópico " + topic, ex);
        }
    }
}