package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.DistractionLogged;
import com.studyanchor.focus.domain.DomainEvent;
import com.studyanchor.focus.domain.EventPublisher;
import com.studyanchor.focus.domain.FocusSessionEnded;
import com.studyanchor.focus.domain.FocusSessionStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Implements FR-05, FR-06 AC4. Adapter: publishes domain events to Kafka as JSON with message key = userId
 * (docs/05-events.md). Binding names map to topics in application.yml.
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
        String binding = bindingFor(event);
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.KEY, event.userId().getBytes(StandardCharsets.UTF_8))
                // FR-12: record time = domain time, so the spike windows count distractions by when they were logged
                .setHeader(KafkaHeaders.TIMESTAMP, event.occurredAt().toEpochMilli())
                .build();
        if (!streamBridge.send(binding, message)) {
            throw new IllegalStateException("Could not publish " + event.eventType());
        }
        log.info("Published {} for user {} to {}", event.eventType(), event.userId(), binding);
    }

    private static String bindingFor(DomainEvent event) {
        return switch (event) {
            case FocusSessionStarted e -> "focusSessions-out-0";
            case FocusSessionEnded e -> "focusSessions-out-0";
            case DistractionLogged e -> "focusDistractions-out-0";
            default -> throw new IllegalArgumentException("No topic for " + event.eventType());
        };
    }
}
