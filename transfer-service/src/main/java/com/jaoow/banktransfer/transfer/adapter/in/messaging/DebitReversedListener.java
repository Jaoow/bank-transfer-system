package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.application.DebitReversedEvent;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitReversedUseCase;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DebitReversedListener {

    private static final Logger log = LoggerFactory.getLogger(DebitReversedListener.class);
    private final HandleDebitReversedUseCase useCase;
    private final ObjectMapper objectMapper;

    public DebitReversedListener(HandleDebitReversedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "debit-reversed", groupId = "transfer-service")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            DebitReversedEvent event = objectMapper.readValue(record.value(), DebitReversedEvent.class);
            log.info("Received DebitReversedEvent for transfer {}", event.transferId());
            useCase.handleDebitReversed(event.transferId());
            ack.acknowledge();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to process DebitReversedEvent", ex);
        }
    }
}
