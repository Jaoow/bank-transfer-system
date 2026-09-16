package com.jaoow.banktransfer.account.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ApplyCreditUseCase {
    void applyCredit(UUID accountId, BigDecimal amount, String idempotencyKey, UUID transferId);
}