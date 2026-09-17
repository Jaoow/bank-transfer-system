package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitReservedUseCase;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DebitReservedListener {

    private final HandleDebitReservedUseCase useCase;
    private final ObjectMapper objectMapper;

    public DebitReservedListener(HandleDebitReservedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "debit-reserved", groupId = "transfer-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        var event = objectMapper.readValue(record.value(), DebitReservedPayload.class);
        useCase.handleDebitReserved(event.transferId(), event.accountId(), event.amount());
        ack.acknowledge();
    }

    public record DebitReservedPayload(java.util.UUID transferId, java.util.UUID accountId, java.math.BigDecimal amount) {}
}