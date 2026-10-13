package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.DomainEvent;
import com.studyanchor.tutor.domain.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Implements FR-02 AC3, FR-07 AC5. Adapter: publishes MaterialAdded to {@code tutor.materials} and
 * UnderstandingRecorded to {@code tutor.understanding} as JSON with message key = userId (docs/05-events.md).
 * Each binding maps to its topic in application.yml.
 */
@Component
class StreamBridgeEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StreamBridgeEventPublisher.class);

    private final StreamBridge streamBridge;

    StreamBridgeEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publish(DomainEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.KEY, event.userId().getBytes(StandardCharsets.UTF_8))
                .build();
        String binding = switch (event.eventType()) {
            case "MaterialAdded" -> "tutorMaterials-out-0";
            case "UnderstandingRecorded" -> "tutorUnderstanding-out-0";
            default -> throw new IllegalArgumentException("No topic for " + event.eventType());
        };
        if (!streamBridge.send(binding, message)) {
            throw new IllegalStateException("Could not publish " + event.eventType());
        }
        log.info("Published {} for user {} to {}", event.eventType(), event.userId(), binding);
    }
}
