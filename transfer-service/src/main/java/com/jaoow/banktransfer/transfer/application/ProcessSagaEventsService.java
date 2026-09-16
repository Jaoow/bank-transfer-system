package com.jaoow.banktransfer.transfer.application;

import com.jaoow.banktransfer.transfer.domain.model.Transfer;
import com.jaoow.banktransfer.transfer.domain.model.TransferStatus;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitFailedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleDebitReservedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.out.EventPublisher;
import com.jaoow.banktransfer.transfer.domain.port.out.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProcessSagaEventsService implements HandleDebitReservedUseCase, HandleDebitFailedUseCase {

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;

    public ProcessSagaEventsService(TransferRepository transferRepository, EventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void handle(UUID transferId, UUID originAccountId, BigDecimal amount) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transferência não encontrada: " + transferId));

        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            return; // já processado — mensagem duplicada, ignora silenciosamente aqui (idempotência via estado)
        }

        transfer.markDebitReserved();
        transferRepository.save(transfer);

        // idempotencyKey do crédito = transferId + ":credit", pra não colidir com a chave do débito
        eventPublisher.publish("credit-requested", transfer.getDestinationAccountId().toString(),
                new CreditRequestedEvent(transferId, transfer.getDestinationAccountId(), amount,
                        transferId + ":credit"));
    }

    @Override
    @Transactional
    public void handle(UUID transferId, String reason) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transferência não encontrada: " + transferId));

        transfer.markCancelled();
        transferRepository.save(transfer);
    }
}