package com.jaoow.banktransfer.transfer.domain.port.out;

import com.jaoow.banktransfer.transfer.domain.model.Transfer;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Optional<Transfer> findById(UUID transferId);

    void save(Transfer transfer);
}