package com.studyanchor.tutor.domain;

/** Port for publishing domain events to Kafka; implemented in infrastructure with StreamBridge. */
public interface EventPublisher {

    void publish(DomainEvent event);
}
