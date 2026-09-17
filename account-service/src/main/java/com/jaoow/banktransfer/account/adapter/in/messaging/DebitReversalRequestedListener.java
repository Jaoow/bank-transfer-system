package com.jaoow.banktransfer.account.adapter.in.messaging;

import tools.jackson.databind.ObjectMapper;
import com.jaoow.banktransfer.account.domain.port.in.ReverseDebitUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DebitReversalRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(DebitReversalRequestedListener.class);
    private final ReverseDebitUseCase reverseDebitUseCase;
    private final ObjectMapper objectMapper;

    public DebitReversalRequestedListener(ReverseDebitUseCase reverseDebitUseCase, ObjectMapper objectMapper) {
        this.reverseDebitUseCase = reverseDebitUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "debit-reversal-requested", groupId = "account-service")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            DebitReversalRequestedEvent event = objectMapper.readValue(record.value(), DebitReversalRequestedEvent.class);
            log.info("Received DebitReversalRequestedEvent for transfer {}: {}", event.transferId(), event);
            
            String idempotencyKey = event.transferId() + ":debit-reversal";
            reverseDebitUseCase.reverseDebit(
                    event.originAccountId(),
                    event.amount(),
                    idempotencyKey,
                    event.transferId()
            );

            ack.acknowledge();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to process DebitReversalRequestedEvent", ex);
        }
    }
}
