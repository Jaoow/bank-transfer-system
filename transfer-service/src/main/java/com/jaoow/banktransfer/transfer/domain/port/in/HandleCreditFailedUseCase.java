package com.jaoow.banktransfer.transfer.domain.port.in;


public interface HandleCreditFailedUseCase {
    void handleCreditFailed(java.util.UUID transferId, java.util.UUID accountId,
                java.math.BigDecimal amount, String reason);
}