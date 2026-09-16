package com.jaoow.banktransfer.transfer.adapter.in.web;

import com.jaoow.banktransfer.transfer.domain.port.in.RequestTransferUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final RequestTransferUseCase requestTransferUseCase;

    public TransferController(RequestTransferUseCase requestTransferUseCase) {
        this.requestTransferUseCase = requestTransferUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransferResponse create(@RequestBody CreateTransferRequest request) {
        UUID transferId = requestTransferUseCase.requestTransfer(
                request.originAccountId(), request.destinationAccountId(), request.amount());
        return new TransferResponse(transferId);
    }

    public record CreateTransferRequest(UUID originAccountId, UUID destinationAccountId, BigDecimal amount) {}
    public record TransferResponse(UUID transferId) {}
}