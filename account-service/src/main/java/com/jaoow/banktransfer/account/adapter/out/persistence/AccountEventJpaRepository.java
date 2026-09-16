package com.jaoow.banktransfer.account.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AccountEventJpaRepository extends JpaRepository<AccountEventJpaEntity, UUID> {
}
