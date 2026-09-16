package com.jaoow.banktransfer.account.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_events")
public class AccountEventJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID accountId;
    private String eventType;
    private BigDecimal amount;
    private Instant createdAt;

    protected AccountEventJpaEntity() {
    }

    public AccountEventJpaEntity(UUID accountId, String eventType, BigDecimal amount) {
        this.accountId = accountId;
        this.eventType = eventType;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getEventType() { return eventType; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
