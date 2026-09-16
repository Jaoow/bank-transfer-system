package com.jaoow.banktransfer.account.adapter.in.messaging;

import com.jaoow.banktransfer.account.domain.port.in.ReverseDebitUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DebitReversalRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(DebitReversalRequestedListener.class);
    private final ReverseDebitUseCase reverseDebitUseCase;

    public DebitReversalRequestedListener(ReverseDebitUseCase reverseDebitUseCase) {
        this.reverseDebitUseCase = reverseDebitUseCase;
    }

    @KafkaListener(topics = "debit-reversal-requested", groupId = "account-service")
    public void listen(DebitReversalRequestedEvent event) {
        log.info("Received DebitReversalRequestedEvent for transfer {}: {}", event.transferId(), event);
        
        String idempotencyKey = event.transferId() + ":debit-reversal";
        reverseDebitUseCase.reverseDebit(
                event.originAccountId(),
                event.amount(),
                idempotencyKey,
                event.transferId()
        );
    }
}
