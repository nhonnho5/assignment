package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.service.TopicCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Implements FR-01 AC10. Reads {@code planner.topics} (from the start on every boot, see application.yml),
 * logs each event and hands it to the topic catalogue.
 */
@Configuration
class PlannerTopicsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlannerTopicsConsumer.class);

    @Bean
    Consumer<PlannerTopicEvent> plannerTopics(TopicCatalogService catalog) {
        return event -> {
            log.info("Received {} for user {}: topic={} course={} name={}", event.eventType(), event.userId(),
                    event.topicId(), event.courseCode(), event.topicName());
            switch (event.eventType()) {
                case "TopicCreated" -> catalog.onTopicCreated(event.toTopicReference());
                case "TopicDeleted" -> catalog.onTopicDeleted(event.topicId());
                default -> log.warn("Ignored unknown event type {}", event.eventType());
            }
        };
    }
}
