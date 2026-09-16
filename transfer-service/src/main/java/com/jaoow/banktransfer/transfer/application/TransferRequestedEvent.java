package com.jaoow.banktransfer.transfer.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestedEvent(UUID transferId, UUID originAccountId, UUID destinationAccountId,
                                       BigDecimal amount, String idempotencyKey) {}