package com.jaoow.banktransfer.transfer.application;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitReversalRequestedEvent(UUID transferId, UUID originAccountId, BigDecimal amount) {}