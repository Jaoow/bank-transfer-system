package com.jaoow.banktransfer.account.adapter.in.web;

import com.jaoow.banktransfer.account.adapter.out.persistence.AccountEventJpaRepository;
import com.jaoow.banktransfer.account.adapter.out.persistence.AccountJpaEntity;
import com.jaoow.banktransfer.account.adapter.out.persistence.AccountJpaRepository;
import com.jaoow.banktransfer.account.adapter.out.persistence.OutboxEventJpaRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class AccountController {

    private final AccountJpaRepository accountRepository;
    private final AccountEventJpaRepository accountEventRepository;
    private final OutboxEventJpaRepository outboxRepository;

    // Lista thread-safe de emitters SSE ativos
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public AccountController(AccountJpaRepository accountRepository,
                             AccountEventJpaRepository accountEventRepository,
                             OutboxEventJpaRepository outboxRepository) {
        this.accountRepository = accountRepository;
        this.accountEventRepository = accountEventRepository;
        this.outboxRepository = outboxRepository;
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> listAccounts() {
        return accountRepository.findAll().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "balance", e.getBalance()
                ))
                .toList();
    }

    @GetMapping("/accounts/{id}/events")
    public List<Map<String, Object>> listAccountEvents(@PathVariable UUID id) {
        return accountEventRepository.findAll().stream()
                .filter(e -> e.getAccountId().equals(id))
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "eventType", e.getEventType(),
                        "amount", e.getAmount(),
                        "createdAt", e.getCreatedAt()
                ))
                .toList();
    }

    /**
     * SSE endpoint: o frontend se conecta e recebe notificações sempre que
     * um novo evento é publicado no outbox (via broadcast pelo OutboxPublisher).
     */
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Envia um heartbeat inicial para confirmar conexão
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /**
     * Chamado pelo OutboxPublisherScheduler sempre que um evento é despachado ao Kafka,
     * para notificar os clientes SSE em tempo real.
     */
    public void broadcastEvent(String topic, String key, String payload) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("kafka-event")
                        .data(Map.of(
                                "topic", topic,
                                "key", key,
                                "payload", payload,
                                "timestamp", Instant.now().toString()
                        )));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
