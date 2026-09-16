package com.jaoow.banktransfer.transfer.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface RequestTransferUseCase {
    UUID requestTransfer(UUID originAccountId, UUID destinationAccountId, BigDecimal amount);
}