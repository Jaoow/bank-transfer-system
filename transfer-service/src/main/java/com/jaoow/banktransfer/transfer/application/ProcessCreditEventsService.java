package com.jaoow.banktransfer.transfer.application;

import com.jaoow.banktransfer.transfer.domain.model.Transfer;
import com.jaoow.banktransfer.transfer.domain.model.TransferStatus;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleCreditAppliedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.in.HandleCreditFailedUseCase;
import com.jaoow.banktransfer.transfer.domain.port.out.EventPublisher;
import com.jaoow.banktransfer.transfer.domain.port.out.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProcessCreditEventsService implements HandleCreditAppliedUseCase, HandleCreditFailedUseCase {

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;

    public ProcessCreditEventsService(TransferRepository transferRepository, EventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void handleCreditApplied(UUID transferId, UUID originAccountId, BigDecimal amount) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transferência não encontrada: " + transferId));

        if (transfer.getStatus() != TransferStatus.DEBIT_RESERVED) {
            return; // transfencia só pode ser completa caso o debito esteja reservado
        }

        transfer.markCompleted();
        transferRepository.save(transfer);
    }


    // quando o crédito falhar
    @Override
    public void handleCreditFailed(UUID transferId, UUID accountId, BigDecimal amount, String reason) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("Transferência não encontrada: " + transferId));

        if (transfer.getStatus() != TransferStatus.DEBIT_RESERVED) {
            return; // já processado — mensagem duplicada, ignora silenciosamente aqui (idempotência via estado)
        }

        transfer.markCompensating();
        transferRepository.save(transfer);

        // TODO: publica debit-reversal-requested para o account-service devolver o valor
        // à conta de origem (account.credit() simétrico ao ApplyCreditService).
        //
        // Falta o fechamento do ciclo de compensação:
        // 1. account-service precisa de um listener no tópico "debit-reversal-requested"
        //    -> chama account.credit(amount) na origem, idempotente (mesma tabela processed_events)
        //    -> em caso de sucesso, publica "debit-reversed"
        //    -> em caso de falha (ex: conta de origem não existe mais), publica "debit-reversal-failed"
        //
        // 2. transfer-service precisa de um DebitReversedListener (HandleDebitReversedUseCase)
        //    -> busca a Transfer, confirma que está em COMPENSATING
        //    -> chama transfer.markCancelled() -> SÓ AQUI o estado final vira CANCELLED de fato
        //
        // 3. caso "debit-reversal-failed" aconteça (compensação falhou):
        //    NÃO tentar automatizar recursivamente. Registrar log.error com transferId + reason
        //    e mover a transferência para um estado RECONCILIATION_REQUIRED (novo enum value)
        //    para intervenção manual — esse é o ponto onde sistemas bancários reais param
        //    de automatizar e escalam para operação humana.

        eventPublisher.publish("debit-reversal-requested", transfer.getOriginAccountId().toString(),
                new DebitReversalRequestedEvent(transferId, transfer.getOriginAccountId(), amount));
    }
}