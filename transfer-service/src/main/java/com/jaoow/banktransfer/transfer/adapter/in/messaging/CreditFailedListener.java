package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.domain.port.in.HandleCreditFailedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitFailedUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CreditFailedListener {

    private final HandleCreditFailedUseCase useCase;
    private final ObjectMapper objectMapper;

    public CreditFailedListener(HandleCreditFailedUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "credit-failed", groupId = "transfer-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        var event = objectMapper.readValue(record.value(), CreditFailedPayload.class);
        useCase.handle(event.transferId(), event.accountId(), event.amount(), event.reason());
        ack.acknowledge();
    }

    public record CreditFailedPayload(java.util.UUID transferId, java.util.UUID accountId,
                                      java.math.BigDecimal amount, String reason) {}
}