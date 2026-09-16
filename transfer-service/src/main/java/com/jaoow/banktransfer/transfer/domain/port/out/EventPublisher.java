package com.jaoow.banktransfer.transfer.domain.port.out;

public interface EventPublisher {
    void publish(String topic, String key, Object event);
}