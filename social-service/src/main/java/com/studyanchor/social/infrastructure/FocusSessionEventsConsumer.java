package com.studyanchor.social.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Implements FR-05 (consumer side). Reads {@code focus.sessions} and logs each event, proving the
 * Kafka pipeline end to end. Replaced by the presence state store in build step 4 (FR-09).
 */
@Configuration
class FocusSessionEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(FocusSessionEventsConsumer.class);

    @Bean
    Consumer<FocusSessionEvent> focusSessionEvents() {
        return event -> log.info("Received {} for user {}: session={} topic={} plannedMinutes={} actualSeconds={}",
                event.eventType(), event.userId(), event.sessionId(), event.topicId(),
                event.plannedMinutes(), event.actualSeconds());
    }
}
