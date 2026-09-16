package com.jaoow.banktransfer.account.adapter.in.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestedEvent(UUID transferId, UUID originAccountId, UUID destinationAccountId,
                                       BigDecimal amount, String idempotencyKey) {}