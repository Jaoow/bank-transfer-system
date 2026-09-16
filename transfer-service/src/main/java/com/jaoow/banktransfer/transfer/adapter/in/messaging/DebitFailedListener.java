package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitFailedUseCase;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DebitFailedListener {

    private final HandleDebitFailedUseCase useCase;
    private final ObjectMapper objectMapper;

    public DebitFailedListener(HandleDebitFailedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "debit-failed", groupId = "transfer-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        var event = objectMapper.readValue(record.value(), DebitFailedPayload.class);
        useCase.handle(event.transferId(), event.reason());
        ack.acknowledge();
    }

    public record DebitFailedPayload(java.util.UUID transferId, java.util.UUID accountId,
                                       java.math.BigDecimal amount, String reason) {}
}