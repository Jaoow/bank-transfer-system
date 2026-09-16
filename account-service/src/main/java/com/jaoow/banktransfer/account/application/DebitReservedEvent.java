package com.jaoow.banktransfer.account.application;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitReservedEvent(UUID transferId, UUID accountId, BigDecimal amount) {}