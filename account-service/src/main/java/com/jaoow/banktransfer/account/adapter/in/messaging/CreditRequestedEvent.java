package com.jaoow.banktransfer.account.adapter.in.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditRequestedEvent(UUID transferId, UUID destinationAccountId, BigDecimal amount, String idempotencyKey) {}