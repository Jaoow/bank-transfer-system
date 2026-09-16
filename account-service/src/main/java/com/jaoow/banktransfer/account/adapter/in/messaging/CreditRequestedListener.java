package com.jaoow.banktransfer.account.adapter.in.messaging;

import com.jaoow.banktransfer.account.domain.port.in.ApplyCreditUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CreditRequestedListener {

    private final ApplyCreditUseCase applyCreditUseCase;
    private final ObjectMapper objectMapper;

    public CreditRequestedListener(ApplyCreditUseCase applyCreditUseCase, ObjectMapper objectMapper) {
        this.applyCreditUseCase = applyCreditUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "credit-requested", groupId = "account-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            CreditRequestedEvent event = objectMapper.readValue(record.value(), CreditRequestedEvent.class);

            applyCreditUseCase.applyCredit(
                    event.destinationAccountId(),
                    event.amount(),
                    event.idempotencyKey(),
                    event.transferId()
            );

            // commit manual SÓ depois que a transação acima já retornou com sucesso
            ack.acknowledge();

        } catch (Exception ex) {
            // não faz ack — a mensagem será reentregue (e cai na DLQ depois de N tentativas, passo futuro)
            throw ex;
        }
    }
}