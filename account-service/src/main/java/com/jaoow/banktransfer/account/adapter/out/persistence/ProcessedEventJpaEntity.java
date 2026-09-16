package com.jaoow.banktransfer.account.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
public class ProcessedEventJpaEntity {

    @Id
    private String idempotencyKey;
    private String eventType;
    private Instant processedAt;

    protected ProcessedEventJpaEntity() {
    }

    public ProcessedEventJpaEntity(String idempotencyKey, String eventType) {
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}