package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.domain.port.in.HandleCreditAppliedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitReservedUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CreditAppliedListener {

    private final HandleCreditAppliedUseCase useCase;
    private final ObjectMapper objectMapper;

    public CreditAppliedListener(HandleCreditAppliedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "credit-applied", groupId = "transfer-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        var event = objectMapper.readValue(record.value(), CreditAppliedPayload.class);
        useCase.handle(event.transferId(), event.accountId(), event.amount());
        ack.acknowledge();
    }

    public record CreditAppliedPayload(java.util.UUID transferId, java.util.UUID accountId, java.math.BigDecimal amount) {}
}