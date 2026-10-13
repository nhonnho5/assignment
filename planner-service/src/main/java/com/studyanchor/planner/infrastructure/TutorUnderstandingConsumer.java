package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/** Implements FR-07 AC14. Reads {@code tutor.understanding}, logs each event and lets the course keep the level. */
@Configuration
class TutorUnderstandingConsumer {

    private static final Logger log = LoggerFactory.getLogger(TutorUnderstandingConsumer.class);

    @Bean
    Consumer<UnderstandingRecordedEvent> tutorUnderstanding(CourseService courses) {
        return event -> {
            log.info("Received {} for user {}: topic={} level={} session={}", event.eventType(), event.userId(),
                    event.topicId(), event.level(), event.tutoringSessionId());
            courses.onUnderstandingRecorded(event.topicId(), event.level(), event.occurredAt());
        };
    }
}
