package com.jaoow.banktransfer.account.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, String> {
}