package com.jaoow.banktransfer.transfer.adapter.out.persistence;

import com.jaoow.banktransfer.transfer.domain.model.Transfer;
import com.jaoow.banktransfer.transfer.domain.port.out.TransferRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;

    public TransferRepositoryAdapter(TransferJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Transfer> findById(UUID transferId) {
        return jpaRepository.findById(transferId)
                .map(entity -> new Transfer(
                        entity.getId(),
                        entity.getOriginAccountId(),
                        entity.getDestinationAccountId(),
                        entity.getAmount(),
                        entity.getStatus()
                ));
    }

    @Override
    public void save(Transfer transfer) {
        TransferJpaEntity entity = jpaRepository.findById(transfer.getId())
                .orElseGet(() -> new TransferJpaEntity(transfer.getId()));
        
        entity.setOriginAccountId(transfer.getOriginAccountId());
        entity.setDestinationAccountId(transfer.getDestinationAccountId());
        entity.setAmount(transfer.getAmount());
        entity.setStatus(transfer.getStatus());
        
        jpaRepository.save(entity);
    }
}
