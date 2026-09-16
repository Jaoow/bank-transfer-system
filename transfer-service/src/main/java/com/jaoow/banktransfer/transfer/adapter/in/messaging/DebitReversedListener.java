package com.jaoow.banktransfer.transfer.adapter.in.messaging;

import com.jaoow.banktransfer.transfer.application.DebitReversedEvent;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitReversedUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DebitReversedListener {

    private static final Logger log = LoggerFactory.getLogger(DebitReversedListener.class);
    private final HandleDebitReversedUseCase useCase;

    public DebitReversedListener(HandleDebitReversedUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "debit-reversed", groupId = "transfer-service")
    public void listen(DebitReversedEvent event) {
        log.info("Received DebitReversedEvent for transfer {}", event.transferId());
        useCase.handle(event.transferId());
    }
}
