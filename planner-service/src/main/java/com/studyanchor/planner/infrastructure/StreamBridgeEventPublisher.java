package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.DomainEvent;
import com.studyanchor.planner.domain.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Implements FR-01 AC4, AC9. Adapter: publishes TopicCreated and TopicDeleted to {@code planner.topics} as JSON
 * with message key = userId (docs/05-events.md). The binding maps to the topic in application.yml.
 */
@Component
class StreamBridgeEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StreamBridgeEventPublisher.class);
    private static final String BINDING = "plannerTopics-out-0";

    private final StreamBridge streamBridge;

    StreamBridgeEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publish(DomainEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.KEY, event.userId().getBytes(StandardCharsets.UTF_8))
                .build();
        if (!streamBridge.send(BINDING, message)) {
            throw new IllegalStateException("Could not publish " + event.eventType());
        }
        log.info("Published {} for user {} to {}", event.eventType(), event.userId(), BINDING);
    }
}
