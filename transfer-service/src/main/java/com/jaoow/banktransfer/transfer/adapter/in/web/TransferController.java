package com.jaoow.banktransfer.transfer.adapter.in.web;

import com.jaoow.banktransfer.transfer.adapter.out.persistence.TransferJpaRepository;
import com.jaoow.banktransfer.transfer.domain.port.in.RequestTransferUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final RequestTransferUseCase requestTransferUseCase;
    private final TransferJpaRepository transferJpaRepository;

    public TransferController(RequestTransferUseCase requestTransferUseCase,
                              TransferJpaRepository transferJpaRepository) {
        this.requestTransferUseCase = requestTransferUseCase;
        this.transferJpaRepository = transferJpaRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransferResponse create(@RequestBody CreateTransferRequest request) {
        UUID transferId = requestTransferUseCase.requestTransfer(
                request.originAccountId(), request.destinationAccountId(), request.amount());
        return new TransferResponse(transferId);
    }

    @GetMapping
    public List<Map<String, Object>> listTransfers() {
        return transferJpaRepository.findAll().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "originAccountId", e.getOriginAccountId(),
                        "destinationAccountId", e.getDestinationAccountId(),
                        "amount", e.getAmount(),
                        "status", e.getStatus()
                ))
                .toList();
    }

    public record CreateTransferRequest(UUID originAccountId, UUID destinationAccountId, BigDecimal amount) {}
    public record TransferResponse(UUID transferId) {}
}