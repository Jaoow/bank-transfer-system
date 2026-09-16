package com.jaoow.banktransfer.account.application;

import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaRepository;
import com.jaoow.banktransfer.account.domain.model.Account;
import com.jaoow.banktransfer.account.domain.model.InsufficientBalanceException;
import com.jaoow.banktransfer.account.domain.port.in.ReserveDebitUseCase;
import com.jaoow.banktransfer.account.domain.port.out.AccountRepository;
import com.jaoow.banktransfer.account.domain.port.out.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReserveDebitService implements ReserveDebitUseCase {

    private final AccountRepository accountRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final EventPublisher eventPublisher;

    public ReserveDebitService(AccountRepository accountRepository,
                                ProcessedEventJpaRepository processedEventRepository,
                                EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void reserveDebit(UUID accountId, BigDecimal amount, String idempotencyKey, UUID transferId) {

        // 1. checagem de idempotência DENTRO da transação
        if (processedEventRepository.existsById(idempotencyKey)) {
            return; // já processado — não faz nada, não republica evento
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));

        try {
            account.debit(amount);
            accountRepository.save(account);
            processedEventRepository.save(new ProcessedEventJpaEntity(idempotencyKey, "DEBIT_RESERVED"));

            // 2. publica DEPOIS de persistir — outbox simplificado (vamos evoluir isso no bônus)
            eventPublisher.publish("debit-reserved", accountId.toString(),
                    new DebitReservedEvent(transferId, accountId, amount));

        } catch (InsufficientBalanceException ex) {
            processedEventRepository.save(new ProcessedEventJpaEntity(idempotencyKey, "DEBIT_FAILED"));
            eventPublisher.publish("debit-failed", accountId.toString(),
                    new DebitFailedEvent(transferId, accountId, amount, ex.getMessage()));
        }
    }
}