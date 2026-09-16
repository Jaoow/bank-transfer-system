package com.jaoow.banktransfer.account.adapter.in.web;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/dlq")
public class DlqController {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ConsumerFactory<Object, Object> consumerFactory;

    public DlqController(KafkaTemplate<Object, Object> kafkaTemplate, ConsumerFactory<Object, Object> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    @PostMapping("/reprocess/{topic}")
    public String reprocess(@PathVariable String topic) {
        String dltTopic = topic + ".DLT";
        int reprocessedCount = 0;

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("dlq-reprocessor", "dlq-client")) {
            consumer.subscribe(java.util.List.of(dltTopic));
            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            for (ConsumerRecord<Object, Object> record : records) {
                kafkaTemplate.send(topic, record.key(), record.value());
                reprocessedCount++;
            }
            consumer.commitSync();
        } catch (Exception e) {
            return "Erro ao reprocessar: " + e.getMessage();
        }

        return "Reprocessadas " + reprocessedCount + " mensagens do tópico " + dltTopic;
    }
}
