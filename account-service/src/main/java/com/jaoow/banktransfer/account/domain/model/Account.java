package com.jaoow.banktransfer.account.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {

    private final UUID id;
    private BigDecimal balance;

    public Account(UUID id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de débito deve ser positivo");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(id, amount, balance);
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de crédito deve ser positivo");
        }
        balance = balance.add(amount);
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}