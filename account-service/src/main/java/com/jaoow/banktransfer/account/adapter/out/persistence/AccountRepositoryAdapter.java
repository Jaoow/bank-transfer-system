package com.jaoow.banktransfer.account.adapter.out.persistence;

import com.jaoow.banktransfer.account.domain.model.Account;
import com.jaoow.banktransfer.account.domain.port.out.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return jpaRepository.findById(accountId)
                .map(entity -> new Account(entity.getId(), entity.getBalance()));
    }

    @Override
    public void save(Account account) {
        AccountJpaEntity entity = jpaRepository.findById(account.getId())
                .orElseThrow(() -> new IllegalStateException("Conta não encontrada: " + account.getId()));
        entity.setBalance(account.getBalance());
        jpaRepository.save(entity);
    }
}