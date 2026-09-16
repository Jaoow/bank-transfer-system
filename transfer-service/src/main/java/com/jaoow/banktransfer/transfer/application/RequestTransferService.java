package com.jaoow.banktransfer.transfer.application;

import com.jaoow.banktransfer.transfer.domain.model.Transfer;
import com.jaoow.banktransfer.transfer.domain.port.in.RequestTransferUseCase;
import com.jaoow.banktransfer.transfer.domain.port.out.EventPublisher;
import com.jaoow.banktransfer.transfer.domain.port.out.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RequestTransferService implements RequestTransferUseCase {

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;

    public RequestTransferService(TransferRepository transferRepository, EventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UUID requestTransfer(UUID originAccountId, UUID destinationAccountId, BigDecimal amount) {

        Transfer transfer = Transfer.request(originAccountId, destinationAccountId, amount);
        transferRepository.save(transfer);

        // idempotencyKey do débito = id da própria transferência.
        // Isso garante que, mesmo se o POST /transfers for chamado 2x por engano
        // (retry de rede, double-click no frontend), o account-service só debita uma vez.
        eventPublisher.publish("transfer-requested", originAccountId.toString(),
                new TransferRequestedEvent(transfer.getId(), originAccountId, destinationAccountId,
                        amount, transfer.getId().toString()));

        return transfer.getId();
    }
}