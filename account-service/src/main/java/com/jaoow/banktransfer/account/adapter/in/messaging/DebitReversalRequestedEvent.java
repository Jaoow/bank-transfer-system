package com.jaoow.banktransfer.account.adapter.in.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitReversalRequestedEvent(UUID transferId, UUID originAccountId, BigDecimal amount) {}
