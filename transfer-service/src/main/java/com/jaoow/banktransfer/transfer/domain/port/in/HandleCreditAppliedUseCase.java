package com.jaoow.banktransfer.transfer.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface HandleCreditAppliedUseCase {
    void handle(UUID transferId, UUID originAccountId, BigDecimal amount);
}