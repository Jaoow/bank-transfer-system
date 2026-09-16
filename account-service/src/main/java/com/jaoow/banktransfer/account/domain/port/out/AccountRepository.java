package com.jaoow.banktransfer.account.domain.port.out;


import com.jaoow.banktransfer.account.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID accountId);

    void save(Account account);
}