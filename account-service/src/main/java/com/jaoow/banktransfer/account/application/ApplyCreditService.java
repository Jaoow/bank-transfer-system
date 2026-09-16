package com.jaoow.banktransfer.account.application;

import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaRepository;
import com.jaoow.banktransfer.account.domain.model.Account;
import com.jaoow.banktransfer.account.domain.port.in.ApplyCreditUseCase;
import com.jaoow.banktransfer.account.domain.port.out.AccountRepository;
import com.jaoow.banktransfer.account.domain.port.out.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ApplyCreditService implements ApplyCreditUseCase {

    private final AccountRepository accountRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final EventPublisher eventPublisher;

    public ApplyCreditService(AccountRepository accountRepository,
                              ProcessedEventJpaRepository processedEventRepository,
                              EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void applyCredit(UUID accountId, BigDecimal amount, String idempotencyKey, UUID transferId) {

        // 1. checagem de idempotência DENTRO da transação
        if (processedEventRepository.existsById(idempotencyKey)) {
            return; // já processado — não faz nada, não republica evento
        }

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));

            account.credit(amount);
            accountRepository.save(account);
            processedEventRepository.save(new ProcessedEventJpaEntity(idempotencyKey, "CREDIT_RESERVED"));

            // 2. publica DEPOIS de persistir — outbox simplificado (vamos evoluir isso no bônus)
            eventPublisher.publish("credit-applied", accountId.toString(),
                    new CreditAppliedEvent(transferId, accountId, amount));

        } catch (IllegalArgumentException ex) {
            processedEventRepository.save(new ProcessedEventJpaEntity(idempotencyKey, "CREDIT_FAILED"));
            eventPublisher.publish("credit-failed", accountId.toString(),
                    new CreditFailedEvent(transferId, accountId, amount, ex.getMessage()));
        }
    }
}