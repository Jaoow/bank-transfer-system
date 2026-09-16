package com.jaoow.banktransfer.transfer.domain.model;

public enum TransferStatus {
    REQUESTED,
    DEBIT_RESERVED,
    COMPLETED,
    COMPENSATING,
    CANCELLED
}