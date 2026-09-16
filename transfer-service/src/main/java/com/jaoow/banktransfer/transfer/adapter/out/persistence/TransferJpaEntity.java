package com.jaoow.banktransfer.transfer.adapter.out.persistence;

import com.jaoow.banktransfer.transfer.domain.model.TransferStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class TransferJpaEntity {

    @Id
    private UUID id;
    
    private UUID originAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @Version
    private Long version;

    protected TransferJpaEntity() {
    }

    public TransferJpaEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(UUID originAccountId) {
        this.originAccountId = originAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }
}
