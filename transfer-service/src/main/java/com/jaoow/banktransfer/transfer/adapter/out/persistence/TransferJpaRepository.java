package com.jaoow.banktransfer.transfer.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {
}
