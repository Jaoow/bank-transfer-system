package com.jaoow.banktransfer.account.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditFailedEvent(UUID transferId, UUID accountId, BigDecimal amount, String reason) {}