package com.jaoow.banktransfer.transfer.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditRequestedEvent(UUID transferId, UUID destinationAccountId, BigDecimal amount, String idempotencyKey) {}