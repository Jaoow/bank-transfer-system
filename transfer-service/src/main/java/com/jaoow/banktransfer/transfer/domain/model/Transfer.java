package com.jaoow.banktransfer.transfer.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Transfer {

    private final UUID id;
    private final UUID originAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private TransferStatus status;

    public Transfer(UUID id, UUID originAccountId, UUID destinationAccountId,
                    BigDecimal amount, TransferStatus status) {
        this.id = id;
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
    }

    public static Transfer request(UUID originAccountId, UUID destinationAccountId, BigDecimal amount) {
        return new Transfer(UUID.randomUUID(), originAccountId, destinationAccountId, amount, TransferStatus.REQUESTED);
    }

    public void markDebitReserved() {
        requireStatus(TransferStatus.REQUESTED);
        this.status = TransferStatus.DEBIT_RESERVED;
    }

    public void markCompleted() {
        requireStatus(TransferStatus.DEBIT_RESERVED);
        this.status = TransferStatus.COMPLETED;
    }

    public void markCompensating() {
        requireStatus(TransferStatus.DEBIT_RESERVED);
        this.status = TransferStatus.COMPENSATING;
    }

    public void markCancelled() {
        if (status != TransferStatus.REQUESTED && status != TransferStatus.COMPENSATING) {
            throw new IllegalStateException(
                    "Transferência " + id + " não pode ser cancelada no estado " + status);
        }
        this.status = TransferStatus.CANCELLED;
    }

    private void requireStatus(TransferStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Transferência " + id + " esperava estado " + expected + " mas está em " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginAccountId() {
        return originAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }
}