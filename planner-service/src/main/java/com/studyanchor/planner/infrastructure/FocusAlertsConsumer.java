package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/** Implements FR-12 AC2. Reads {@code focus.alerts}, logs each event and lets the reminder service react. */
@Configuration
class FocusAlertsConsumer {

    private static final Logger log = LoggerFactory.getLogger(FocusAlertsConsumer.class);

    @Bean
    Consumer<DistractionSpikeEvent> focusAlerts(ReminderService reminders) {
        return event -> {
            log.info("Received {} for user {}: {} distractions in {} – {}", event.eventType(), event.userId(),
                    event.distractionCount(), event.windowStart(), event.windowEnd());
            reminders.onDistractionSpike(event.userId(), event.distractionCount(), event.windowStart());
        };
    }
}
