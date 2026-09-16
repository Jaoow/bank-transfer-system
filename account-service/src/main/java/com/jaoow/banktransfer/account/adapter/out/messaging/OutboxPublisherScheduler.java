package com.jaoow.banktransfer.account.adapter.out.messaging;

import com.jaoow.banktransfer.account.adapter.out.persistence.OutboxEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);
    
    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherScheduler(OutboxEventJpaRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void publishEvents() {
        List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        
        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).get(); // sync send to ensure order
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Published outbox event to topic {}: {}", event.getTopic(), event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}. Will retry later.", event.getId(), e);
                break; // Stop processing to maintain order if one fails
            }
        }
    }
}
