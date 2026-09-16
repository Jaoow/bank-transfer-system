package com.jaoow.banktransfer.account.application;

import com.jaoow.banktransfer.account.adapter.out.persistence.AccountEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.AccountEventJpaRepository;
import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.ProcessedEventJpaRepository;
import com.jaoow.banktransfer.account.domain.model.Account;
import com.jaoow.banktransfer.account.domain.port.in.ReverseDebitUseCase;
import com.jaoow.banktransfer.account.domain.port.out.AccountRepository;
import com.jaoow.banktransfer.account.domain.port.out.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReverseDebitService implements ReverseDebitUseCase {

    private final AccountRepository accountRepository;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final AccountEventJpaRepository accountEventJpaRepository;
    private final EventPublisher eventPublisher;

    public ReverseDebitService(AccountRepository accountRepository,
                               ProcessedEventJpaRepository processedEventRepository,
                               AccountEventJpaRepository accountEventJpaRepository,
                               EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.processedEventRepository = processedEventRepository;
        this.accountEventJpaRepository = accountEventJpaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void reverseDebit(UUID accountId, BigDecimal amount, String idempotencyKey, UUID transferId) {

        if (processedEventRepository.existsById(idempotencyKey)) {
            return; // já processado
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));

        account.credit(amount);
        accountRepository.save(account);
        accountEventJpaRepository.save(new AccountEventJpaEntity(accountId, "CREDIT_REVERSAL", amount));
        processedEventRepository.save(new ProcessedEventJpaEntity(idempotencyKey, "DEBIT_REVERSED"));

        eventPublisher.publish("debit-reversed", accountId.toString(),
                new DebitReversedEvent(transferId, accountId, amount));
    }
}
