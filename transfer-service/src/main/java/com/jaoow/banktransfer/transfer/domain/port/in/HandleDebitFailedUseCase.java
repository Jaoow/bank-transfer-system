package com.jaoow.banktransfer.transfer.domain.port.in;

import java.util.UUID;

public interface HandleDebitFailedUseCase {
    void handleDebitFailed(UUID transferId, String reason);
}