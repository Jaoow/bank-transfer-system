package com.jaoow.banktransfer.transfer.domain.port.in;

import java.util.UUID;

public interface HandleDebitReversedUseCase {
    void handleDebitReversed(UUID transferId);
}
