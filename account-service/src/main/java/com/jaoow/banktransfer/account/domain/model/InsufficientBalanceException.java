package com.jaoow.banktransfer.account.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID accountId, BigDecimal requested, BigDecimal available) {
        super("Conta " + accountId + " tem saldo insuficiente: solicitado=" + requested + ", disponível=" + available);
    }
}