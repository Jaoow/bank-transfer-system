package com.jaoow.banktransfer.account.adapter.in.messaging;

import com.jaoow.banktransfer.account.domain.port.in.ReserveDebitUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransferRequestedListener {

    private final ReserveDebitUseCase reserveDebitUseCase;
    private final ObjectMapper objectMapper;

    public TransferRequestedListener(ReserveDebitUseCase reserveDebitUseCase, ObjectMapper objectMapper) {
        this.reserveDebitUseCase = reserveDebitUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transfer-requested", groupId = "account-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            TransferRequestedEvent event = objectMapper.readValue(record.value(), TransferRequestedEvent.class);

            reserveDebitUseCase.reserveDebit(
                    event.originAccountId(),
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