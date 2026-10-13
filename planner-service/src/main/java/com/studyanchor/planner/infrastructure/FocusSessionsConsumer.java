package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.service.StudyPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/** Implements FR-12 AC3, AC12. Reads {@code focus.sessions}; a session linked to a time block updates that block. */
@Configuration
class FocusSessionsConsumer {

    private static final Logger log = LoggerFactory.getLogger(FocusSessionsConsumer.class);

    @Bean
    Consumer<FocusSessionEvent> focusSessions(StudyPlanService plans) {
        return event -> {
            if (event.timeBlockId() == null) {
                return;
            }
            log.info("Received {} for user {}: session={} timeBlock={}", event.eventType(), event.userId(),
                    event.sessionId(), event.timeBlockId());
            switch (event.eventType()) {
                case "FocusSessionStarted" -> plans.onFocusSessionStarted(event.userId(), event.timeBlockId());
                case "FocusSessionEnded" -> plans.onFocusSessionEnded(event.userId(), event.timeBlockId());
                default -> log.warn("Ignored unknown event type {}", event.eventType());
            }
        };
    }
}
